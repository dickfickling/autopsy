/*
 * Autopsy Forensic Browser
 *
 * Copyright 2011 Basis Technology Corp.
 * Contact: carrier <at> sleuthkit <dot> org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.sleuthkit.autopsy.filesearch;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import javax.swing.ListSelectionModel;
import junit.framework.Test;
import org.netbeans.jellytools.JellyTestCase;
import org.netbeans.jellytools.NbDialogOperator;
import org.netbeans.jellytools.TopComponentOperator;
import org.netbeans.jellytools.WizardOperator;
import org.netbeans.jellytools.actions.ActionNoBlock;
import org.netbeans.jemmy.Timeout;
import org.netbeans.jemmy.operators.JButtonOperator;
import org.netbeans.jemmy.operators.JCheckBoxOperator;
import org.netbeans.jemmy.operators.JScrollPaneOperator;
import org.netbeans.jemmy.operators.JTextFieldOperator;
import org.netbeans.jemmy.operators.JTreeOperator;
import org.netbeans.junit.NbModuleSuite;
import org.netbeans.swing.outline.Outline;
import org.openide.explorer.view.OutlineView;
import org.openide.explorer.view.Visualizer;
import org.openide.nodes.Node;
import org.sleuthkit.autopsy.coreutils.AutopsyPropFile;
import org.sleuthkit.autopsy.directorytree.DirectoryTreeFilterNode;

/**
 *
 * @author dfickling
 */
public class FileSearchTest extends JellyTestCase{
    
    /** Constructor required by JUnit */
    public FileSearchTest(String name) {
        super(name);
    }
    
    
    /** Creates suite from particular test cases. */
    public static Test suite() {
        // run tests in particular order
        //return createModuleTest(EmptyTest.class, "test2", "test1");

        // run all tests 
        //return createModuleTest(EmptyTest.class);

        // run tests with specific configuration
        NbModuleSuite.Configuration conf = NbModuleSuite.createConfiguration(FileSearchTest.class).
                clusters(".*").
                enableModules(".*").
                failOnException(Level.INFO).
                failOnMessage(Level.SEVERE);
        conf = conf.addTest("testNewCaseWizardOpen",
                "testNewCaseWizard",
                "testAddImageWizard1",
                "testDirectoryTree1",
                "testAddImageWizard2",
                "testDirectoryTree2",
                "testFileSearch1",
                "testFileSearchResult1");
        return NbModuleSuite.create(conf);
    }

    /** Method called before each test case. */
    @Override
    public void setUp() {
        System.out.println("########  " + getName() + "  #######");
    }

    /** Method called after each test case. */
    @Override
    public void tearDown() {
    }
    
    public void testNewCaseWizardOpen() {
        System.out.println("New Case");
        NbDialogOperator nbdo = new NbDialogOperator("Welcome");
        JButtonOperator jbo = new JButtonOperator(nbdo, 2); // the "New Case" button
        jbo.clickMouse();
        
    }
    
    public void testNewCaseWizard() {
        System.out.println("New Case Wizard");
        WizardOperator wo = new WizardOperator("New Case Information");
        JTextFieldOperator jtfo1 = new JTextFieldOperator(wo, 1);
        jtfo1.typeText("AutopsyTestCase"); // Name the case "AutopsyTestCase"
        JTextFieldOperator jtfo0 = new JTextFieldOperator(wo, 0);
        jtfo0.typeText(AutopsyPropFile.getUserDirPath().toString()); // Put the case in the test user directory
        wo.btFinish().clickMouse();
    }
    
    public void testAddImageWizard1() {
        System.out.println("AddImageWizard 1");
        WizardOperator wo = new WizardOperator("Add Image");
        JTextFieldOperator jtfo0 = new JTextFieldOperator(wo, 0);
        String imageDir = "Y:\\fe_test_3.img"; //oh dear god fix this
        jtfo0.typeText(imageDir);
        JCheckBoxOperator jcbo = new JCheckBoxOperator(wo, 1); // enable keyword search indexing
        jcbo.clickMouse();
        wo.btNext().clickMouse();
        new Timeout("pausing", 5000).sleep(); // give it a second (or five) to process
        wo.btNext().clickMouse();
        new Timeout("pausing", 5000).sleep();
        wo.btFinish().clickMouse();
    }
    
    public void testDirectoryTree1() {
        System.out.println("Directory Tree 1");
        TopComponentOperator tcOper = new TopComponentOperator("Directory Tree");
        tcOper.makeComponentVisible();
    }
    
    public void testAddImageWizard2() {
        System.out.println("AddImage Wizard 2");
        new ActionNoBlock("File|Add Image...", null).perform();
        WizardOperator wo = new WizardOperator("Add Image");
        JTextFieldOperator jtfo0 = new JTextFieldOperator(wo, 0);
        String imageDir = "fe_test_4.img"; //oh dear god fix this
        jtfo0.typeText(imageDir);
        JCheckBoxOperator jcbo = new JCheckBoxOperator(wo, 1);
        jcbo.clickMouse();
        wo.btNext().clickMouse();
        new Timeout("pausing", 5000).sleep();
        wo.btNext().clickMouse();
        new Timeout("pausing", 5000).sleep();
        wo.btFinish().clickMouse();
    }
    
    public void testDirectoryTree2() {
        System.out.println("Directory Tree 2");
        TopComponentOperator tcOper = new TopComponentOperator("Directory Tree");
        tcOper.makeComponentVisible();
        JTreeOperator jto = new JTreeOperator(tcOper);
        jto.setSelectionRow(4); // Select the second image
        DirectoryTreeFilterNode root = (DirectoryTreeFilterNode) Visualizer.findNode(jto.getRoot());
        for(int i=0; i<root.getChildren().getNodesCount(); i++){
            System.out.println(root.getChildren().getNodeAt(i).getDisplayName());
        }
    }
    
    public void testFileSearch1() {
        System.out.println("File search 1");
        TopComponentOperator tcOper = new TopComponentOperator("File Search");
        tcOper.makeComponentVisible();
        JTextFieldOperator jtfo0 = new JTextFieldOperator(tcOper, 0);
        jtfo0.clickMouse();
        jtfo0.typeText("maytag"); // file search for the word "maytag"
        JButtonOperator jbo = new JButtonOperator(tcOper, "Search");
        jbo.clickMouse();
    }
    
   public void testFileSearchResult1() {
        System.out.println("Select file search result 1");
        TopComponentOperator tcOper = new TopComponentOperator("File Search Results 1");
        tcOper.makeComponentVisible();
        // What follows is some dark voodoo magic to procure the search results (list of nodes)
        JScrollPaneOperator jspo = new JScrollPaneOperator(tcOper);
        OutlineView ov = (OutlineView) jspo.getSource();
        List<Node> searchResults = new ArrayList<Node>(); 
        Outline outline = ov.getOutline(); 
        ListSelectionModel lsm = outline.getSelectionModel();
        lsm.setSelectionInterval(0, 0); // to select the first row
        int numRows = outline.getRowCount(); 
        for (int i=0; i < numRows; i++) { 
            Object itemInZerothColumn = outline.getValueAt(i, 0); 
            Node myNode = Visualizer.findNode(itemInZerothColumn);
            searchResults.add(myNode); 
        }
        // Perhaps here we can compare searchResults with an expected result
        assertEquals(4, searchResults.size());
    }
}
