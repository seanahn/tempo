/**
 * Copyright (c) 2005-2006 Intalio inc.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Intalio inc. - initial API and implementation
 *
 * $Id: TaskManagementServicesFacade.java 5440 2006-06-09 08:58:15Z imemruk $
 * $Log:$
 */

package org.intalio.tempo.workflow.task.xml;

import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;

import junit.framework.TestCase;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.intalio.tempo.workflow.task.PATask;
import org.intalio.tempo.workflow.task.Task;
import org.intalio.tempo.workflow.util.xml.InvalidInputFormatException;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

public class TaskUnmarshallerTest extends TestCase {

    private static final Logger _logger = LoggerFactory.getLogger(TaskUnmarshallerTest.class);

    public static void main(String[] args) {
        junit.textui.TestRunner.run(TaskUnmarshallerTest.class);
    }

    //
    // public void testMetadataUnmarshalling() throws Exception {
    // testMetadata("/taskMetadata.xml");
    // }

    private void testMetadata(String file) throws Exception {
        TaskUnmarshaller unmarshaller = new TaskUnmarshaller();
        TaskMarshaller marshaller = new TaskMarshaller();

        OMElement rootElement = TestUtils.loadElementFromResource(file);

        Iterator<OMElement> i = rootElement.getChildElements();
        while (i.hasNext()) {
            OMElement metadataElement = i.next();
            Task task = unmarshaller.unmarshalTaskFromMetadata(metadataElement);
            OMElement remarshalledMetadata = marshaller.marshalTaskMetadata(task, null);
            _logger.debug(TestUtils.toPrettyXML(remarshalledMetadata));
        }
    }

    private void testBadTaskMetadata(String resourceName) throws Exception {
        TaskUnmarshaller unmarshaller = new TaskUnmarshaller();

        OMElement rootElement = TestUtils.loadElementFromResource(resourceName);
        try {
            unmarshaller.unmarshalTaskFromMetadata(rootElement);
            Assert.fail("InvalidInputFormatException expected:" + resourceName);
        } catch (InvalidInputFormatException e) {
            _logger.debug("Expected exception OK.\nMessage: " + e.getMessage());
        }
    }

    public void testBadTasksMetadata() throws Exception {
        for (int i = 1; i <= 14; ++i) {
            this.testBadTaskMetadata("/badTask" + i + ".xml");
        }
    }

    private void testFullTask(String resourceName) throws Exception {
        TaskUnmarshaller unmarshaller = new TaskUnmarshaller();
        OMFactory factory = OMAbstractFactory.getOMFactory();
        TaskMarshaller marshaller = new TaskMarshaller();
        OMElement rootElement = TestUtils.loadElementFromResource(resourceName);

        Task task = unmarshaller.unmarshalFullTask(rootElement);
        OMElement remarshalledTask = factory.createOMElement("task", TaskXMLConstants.TASK_NAMESPACE, TaskXMLConstants.TASK_NAMESPACE_PREFIX);

        marshaller.marshalFullTask(task, remarshalledTask, null);
        _logger.debug(TestUtils.toPrettyXML(remarshalledTask));
    }

    public void testFullTasks() throws Exception {
        this.testFullTask("/fullPATask1.xml");
        this.testFullTask("/fullPATask2.xml");
        this.testFullTask("/fullPATask3.xml");
        this.testFullTask("/fullPATask4.xml");
        this.testFullTask("/fullPIPATask1.xml");
    }

    public void testOracleTask() throws Exception {
        TaskUnmarshaller unmarshaller = new TaskUnmarshaller();
        String[] oracleTests = new String[] { "/oracle.xml", "/failInOracle.xml", "/failInOracle2.xml", "/failInOracle3.xml" };
        for (String test : oracleTests) {
            OMElement rootElement = TestUtils.loadElementFromResource(test);
            unmarshaller.unmarshalTaskFromMetadata(rootElement);
        }
    }

    private void testBadFullTask(String resourceName) throws Exception {
        TaskUnmarshaller unmarshaller = new TaskUnmarshaller();
        OMElement rootElement = TestUtils.loadElementFromResource(resourceName);

        try {
            unmarshaller.unmarshalFullTask(rootElement);
            Assert.fail("InvalidInputFormatException expected (" + resourceName + ")");
        } catch (InvalidInputFormatException e) {
            _logger.debug("Expected exception OK.\nMessage: " + e.getMessage());
        }
    }

    public void testBadFullTasks() throws Exception {
        this.testBadFullTask("/badFullPATask1.xml");
        this.testBadFullTask("/badFullPIPATask1.xml");
        this.testBadFullTask("/multiElementPayloadPATask1.xml");
    }

    public void testUnmarshalTaskOutput() throws Exception {
        OMElement rootElement = TestUtils.loadElementFromResource("/taskOutput.xml");
        TaskUnmarshaller unmarshaller = new TaskUnmarshaller();
        Document op = unmarshaller.unmarshalTaskOutput(rootElement);
        _logger.debug(new XmlTooling().serializeXML(op));
    }

    public void testUnmarshalTaskWithCreationDate() throws Exception {
        checkTaskDates(new Object[] { "/taskWithDates1.xml", 2008, 2008 });
        checkTaskDates(new Object[] { "/taskWithDates2.xml", 2008, 2008 });
        checkTaskDates(new Object[] { "/taskWithDates3.xml", Calendar.getInstance().get(Calendar.YEAR), -1 }); // using
                                                                                                               // -1
                                                                                                               // as
                                                                                                               // null
    }

    private void checkTaskDates(Object[] date1) throws Exception {
        PATask task = (PATask) new TaskUnmarshaller().unmarshalFullTask(TestUtils.loadElementFromResource((String) date1[0]));
        checkYear(task.getCreationDate(), (Integer) date1[1]);
        checkYear(task.getDeadline(), (Integer) date1[2]);
    }

    private void checkYear(Date creationDate, int year) {
        if (creationDate != null) {
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(creationDate.getTime());
            assertEquals(c.get(Calendar.YEAR), year);
        } else
            assertEquals(year, -1);
    }
}
