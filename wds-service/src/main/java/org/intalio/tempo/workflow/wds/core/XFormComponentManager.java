/**
 * Copyright (c) 2008 Intalio inc.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Intalio inc. - initial API and implementation
 */

package org.intalio.tempo.workflow.wds.core;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.intalio.tempo.deployment.ComponentId;
import org.intalio.tempo.deployment.DeploymentMessage;
import org.intalio.tempo.deployment.DeploymentMessage.Level;
import org.intalio.tempo.deployment.spi.ComponentManagerResult;
import org.intalio.tempo.security.token.TokenContext;
import org.intalio.tempo.workflow.wds.core.xforms.XFormsProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XFormComponentManager implements org.intalio.tempo.deployment.spi.ComponentManager {
    private static final Logger LOG = LoggerFactory.getLogger(XFormComponentManager.class);

    private WDSServiceFactory _wdsFactory;

    public XFormComponentManager(WDSServiceFactory wdsFactory) {
        _wdsFactory = wdsFactory;
    }

    // ------------------ ComponentManager implementation ------------------------

    public String getComponentManagerName() {
        return "xform";
    }

    public void activate(ComponentId name, File path) {
        // nothing
    }

    public void deactivate(ComponentId name) {
        // nothing
    }

    public ComponentManagerResult deploy(ComponentId name, File base) {
        List<DeploymentMessage> msgs = new ArrayList<DeploymentMessage>();

        /*
         * ALEX: Disabled until we get token propagation from deploy-impl if (!TokenContext.hasToken()) { msgs.add(new
         * DeploymentMessage(Level.ERROR, "No security context token")); return msgs; }
         */

        String token = TokenContext.getToken();

        /* ALEX: */
        token = "x";

        WDSService wds = _wdsFactory.getWDSService();
        try {
            // Phase 1: Check for conflicts
            checkDir(base, base, msgs, token, wds);

            // Stop if any error during checks
            for (DeploymentMessage msg : msgs) {
                if (Level.ERROR.equals(msg.getLevel()))
                    return new ComponentManagerResult(msgs);
            }

            // Phase 2: Actual deployment
            processDir(base, base, msgs, token, wds);
        } finally {
            wds.close();
        }
        return new ComponentManagerResult(msgs);
    }

    public void undeploy(ComponentId name, List<String> deployedObjects) {
        // TODO: We need to record which XForms and Items were deployed for this component.
    }

    public void start(ComponentId name) {
        // nothing
    }

    public void stop(ComponentId name) {
        // nothing
    }

    // ------------------ Common deployment methods ------------------------


    public DeploymentMessage checkItem(String token, WDSService wds, InputStream input, String itemURL) {
        DeploymentMessage msg = null;
        try {
            Item existing = wds.retrieveItem(itemURL, token);
            if (existing != null) {
                // ALEX: Disabled until undeploy is implemented
                // msg = new DeploymentMessage(Level.ERROR, "Item already exists: " + itemURL);
                // msg.setResource(itemURL);
            }
        } catch (UnavailableItemException e) {
            // doesn't exist, continue
        } catch (Exception e) {
            LOG.error("Error while checking item: " + itemURL, e);
            msg = new DeploymentMessage(Level.ERROR, e.toString());
            msg.setResource(itemURL);
        }
        return msg;
    }

    public DeploymentMessage processItem(String token, WDSService wds, InputStream input, String itemURL) {
        DeploymentMessage msg = null;
        try {
            LOG.debug("Store Item {}", itemURL);
            Item item = new Item(itemURL, "application/xml", copyToByteArray(input));
            wds.storeItem(item, token);
        } catch (Exception e) {
            LOG.error("Error while storing item: " + itemURL, e);
            msg = new DeploymentMessage(Level.ERROR, e.toString());
            msg.setResource(itemURL);
        }
        return msg;
    }

    public DeploymentMessage checkXForm(String token, WDSService wds, InputStream stream, String formURL) {
        return checkItem(token, wds, stream, formURL);
    }

    public DeploymentMessage processXForm(String token, WDSService wds, InputStream stream, String formURL) {
        DeploymentMessage msg = null;
        try {
            LOG.debug("Store XForm {}", formURL);
            Item item = XFormsProcessor.processXForm(formURL, new ByteArrayInputStream(copyToByteArray(stream)));
            wds.storeItem(item, token);
            return null;
        } catch (Exception e) {
            LOG.error("Error while storing XForm: " + formURL, e);
            msg = new DeploymentMessage(Level.ERROR, e.toString());
            msg.setResource(formURL);
        }
        return msg;
    }

    // ------------------ Private stuff ------------------------

    private void checkDir(File base, File dir, List<DeploymentMessage> msgs, String token, WDSService wds) {
        File[] files = dir.listFiles();
        for (File f : files) {
            LOG.debug("Check: {}", f);
            if (f.isDirectory()) {
                checkDir(base, f, msgs, token, wds);
            } else if (f.isFile()) {
                try {
                    DeploymentMessage msg = null;
                    InputStream input = new FileInputStream(f);
                    try {
                        if (f.getName().endsWith(".xform")) {
                            msg = checkXForm(token, wds, input, relativePath(base, f));
                        } else {
                            msg = checkItem(token, wds, input, relativePath(base, f));
                        }
                        if (msg != null)
                            msgs.add(msg);
                    } finally {
                        close(input);
                    }
                } catch (Exception e) {
                    LOG.error("Error while checking item: " + f, e);
                    DeploymentMessage msg = new DeploymentMessage(Level.ERROR, e.toString());
                    msg.setResource(relativePath(base, f));
                    msgs.add(msg);
                }
            } else {
                DeploymentMessage msg = new DeploymentMessage(Level.WARNING, "Unknown file type: " + f);
                msg.setResource(relativePath(base, f));
                msgs.add(msg);
            }
        }
    }

    private void processDir(File base, File dir, List<DeploymentMessage> msgs, String token, WDSService wds) {
        File[] files = dir.listFiles();
        for (File f : files) {
            LOG.debug("Process: {}", f);
            if (f.isDirectory()) {
                processDir(base, f, msgs, token, wds);
            } else if (f.isFile()) {
                try {
                    DeploymentMessage msg = null;
                    InputStream input = new FileInputStream(f);
                    try {
                        if (f.getName().endsWith(".xform")) {
                            msg = processXForm(token, wds, input, relativePath(base, f));
                        } else {
                            msg = processItem(token, wds, input, relativePath(base, f));
                        }
                        if (msg != null)
                            msgs.add(msg);
                    } finally {
                        close(input);
                    }
                } catch (Exception e) {
                    // this shouldn't happen but if it does, fail fast
                    LOG.error("Error while processing item: " + f, e);
                    DeploymentMessage msg = new DeploymentMessage(Level.ERROR, e.toString());
                    msg.setResource(relativePath(base, f));
                    msgs.add(msg);
                    break;
                }
            } else {
                DeploymentMessage msg = new DeploymentMessage(Level.WARNING, "Unknown file type: " + f);
                msg.setResource(relativePath(base, f));
                msgs.add(msg);
            }
        }
    }

    /**
     * Copy the content of the stream to a byte array
     */
    private static byte[] copyToByteArray(InputStream input) throws IOException {
        byte[] bytes = new byte[32768];
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        while (true) {
            int bytesRead = input.read(bytes);
            if (bytesRead < 0)
                break;
            output.write(bytes, 0, bytesRead);
        }
        return output.toByteArray();
    }

    private static String relativePath(File parent, File child) {
        return parent.toURI().relativize(child.toURI()).toString();
    }

    private static void close(Closeable c) {
        try {
            c.close();
        } catch (Exception e) {
            // ignore
        }
    }
}