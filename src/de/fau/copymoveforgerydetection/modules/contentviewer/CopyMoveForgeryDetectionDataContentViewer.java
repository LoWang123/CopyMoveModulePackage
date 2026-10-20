/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.fau.copymoveforgerydetection.modules.contentviewer;

import java.io.File;
import de.fau.copymoveforgerydetection.modules.ingestModule.CopyMoveIngestModuleFactory;
import java.awt.Component;
import java.awt.image.BufferedImage;
import java.awt.Image;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import org.openide.util.lookup.ServiceProvider;
import org.sleuthkit.autopsy.corecomponentinterfaces.DataContentViewer;
import org.openide.nodes.Node;
import org.sleuthkit.autopsy.coreutils.Logger;
import org.sleuthkit.datamodel.AbstractFile;
import org.sleuthkit.datamodel.BlackboardArtifact;
import org.sleuthkit.datamodel.BlackboardAttribute;
import org.sleuthkit.datamodel.TskCoreException;
/**
 *
 * @author Tobi
 */
@ServiceProvider(service = DataContentViewer.class)
public class CopyMoveForgeryDetectionDataContentViewer extends javax.swing.JPanel implements DataContentViewer{
    
    private String currentName = "";
    private String currentFileExtension = "";
    private BufferedImage currentImage = null;
    private final JFileChooser fileChooser = new JFileChooser();
    private static final Logger logger = Logger.getLogger("CopyMoveForgeryDetectionDataContentViewer");
    
    /**
     * Creates new form CopyMoveForgeryDetectionDataContentViewer
     */
    public CopyMoveForgeryDetectionDataContentViewer() {
        initComponents();
    }

    
    @Override
    public void setNode(Node selectedNode) {
        // Autopsy calls setNode(null) to clear the viewer when the selection is cleared.
        if (selectedNode == null) {
            resetComponent();
            return;
        }
        AbstractFile abstractFile = selectedNode.getLookup().lookup(AbstractFile.class);
        if (abstractFile == null) {
            resetComponent();
            return;
        }
        try {
            initializeResultImage(abstractFile);
        }
        catch (IOException ex) {
            logger.log(Level.SEVERE, "Error when trying to read Image from Blackboard (setNode)", ex);
        }
        catch (TskCoreException ex) {
            logger.log(Level.SEVERE, "Error when trying to access Blackboard (setNode)", ex);
        }
        catch (Exception ex) {
            logger.log(Level.SEVERE, "Unknown exception (setNode)", ex);
        }
    }

    /** True if the attribute is the custom result-image attribute. */
    private static boolean isResultImage(BlackboardAttribute attribute) {
        return CopyMoveIngestModuleFactory.getResultAttributeTypeString()
                .equals(attribute.getAttributeType().getTypeName());
    }

    private void initializeResultImage(AbstractFile abstractFile) throws TskCoreException, IOException {

        BufferedImage resultImage = null;

        for (BlackboardArtifact artifact : abstractFile.getArtifacts(BlackboardArtifact.ARTIFACT_TYPE.TSK_INTERESTING_FILE_HIT)) {
            for (BlackboardAttribute attribute : artifact.getAttributes()) {
                if (isResultImage(attribute)) {
                    resultImage = ImageIO.read(new ByteArrayInputStream(attribute.getValueBytes()));
                }
            }
        }

        String ext = abstractFile.getNameExtension();
        if (ext == null) {
            ext = "";
        }
        currentFileExtension = ext.isEmpty() ? "png" : ext;
        currentName = abstractFile.getName();
        if (!ext.isEmpty() && currentName.endsWith("." + ext)) {
            currentName = currentName.substring(0, currentName.length() - ext.length() - 1);
        }
        currentName += "_CopyMoveResult";

        labelResultText.setText(currentName);
        setImage(resultImage);
    }

    private void setImage(BufferedImage image) {
        currentImage = image;
        if (image == null) {
            labelResultImage.setIcon(null);
            return;
        }

        Image scaledImage = image;
        int panelWidth = panelImage.getWidth();
        int panelHeight = panelImage.getHeight();

        // Before the panel is laid out its size is 0; don't scale (avoids divide-by-zero).
        if (panelWidth > 0 && panelHeight > 0) {
            double differenceWidth = (double) image.getWidth() / (double) panelWidth;
            double differenceHeight = (double) image.getHeight() / (double) panelHeight;

            if (differenceWidth > differenceHeight) {
                if (differenceWidth > 1) {
                    int height = Math.max(1, (int) ((double) image.getHeight() / differenceWidth));
                    scaledImage = image.getScaledInstance(panelWidth, height, Image.SCALE_FAST);
                }
            }
            else if (differenceHeight > 1) {
                int width = Math.max(1, (int) ((double) image.getWidth() / differenceHeight));
                scaledImage = image.getScaledInstance(width, panelHeight, Image.SCALE_FAST);
            }
        }

        labelResultImage.setIcon(new ImageIcon(scaledImage));
    }


    @Override
    public String getTitle() {
        return "Copy Move Forgery Detection";
    }

    @Override
    public String getToolTip() {
        return "Displays the results from the CopyMoveIngestModule.";
    }

    @Override
    public DataContentViewer createInstance() {
        return new CopyMoveForgeryDetectionDataContentViewer();
    }

    @Override
    public Component getComponent() {
        return this;
    }

    @Override
    public void resetComponent() {
        currentImage = null;
        labelResultImage.setIcon(null);
        labelResultText.setText("");
    }

    @Override
    public boolean isSupported(Node node) {
        if (node == null) {
            return false;
        }
        AbstractFile abstractFile = node.getLookup().lookup(AbstractFile.class);
        return isSupported(abstractFile);
    }

    private boolean isSupported(AbstractFile abstractFile) {

        if (abstractFile == null) {
            return false;
        }
        try {
            for (BlackboardArtifact artifact : abstractFile.getArtifacts(BlackboardArtifact.ARTIFACT_TYPE.TSK_INTERESTING_FILE_HIT)) {
                for (BlackboardAttribute attribute : artifact.getAttributes()) {
                    if (isResultImage(attribute)) {
                        return true;
                    }
                }
            }
        }
        catch (TskCoreException ex) {
            logger.log(Level.SEVERE, "Error when trying to read BlackboardAttributes (isSupported)", ex);
        }
        return false;
    }
    
    @Override
    public int isPreferred(Node node) {
        if (node == null) {
            return 0;
        }
        AbstractFile abstractFile = node.getLookup().lookup(AbstractFile.class);
        if(isSupported(abstractFile)) {
            return 2;
        }
        return 0;
    }
    
    class resizeListener extends ComponentAdapter {
        @Override
        public void componentResized(ComponentEvent e) {            
            setImage(currentImage);
        }
    }
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        jPanel1 = new javax.swing.JPanel();
        labelResultText = new javax.swing.JLabel();
        panelImage = new javax.swing.JPanel();
        labelResultImage = new javax.swing.JLabel();
        jButton1 = new javax.swing.JButton();
        filler1 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 0), new java.awt.Dimension(32767, 0));

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 100, Short.MAX_VALUE)
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 100, Short.MAX_VALUE)
        );

        setBackground(new java.awt.Color(0, 0, 0));
        setLayout(new java.awt.GridBagLayout());

        labelResultText.setBackground(new java.awt.Color(0, 0, 0));
        labelResultText.setForeground(new java.awt.Color(255, 255, 255));
        org.openide.awt.Mnemonics.setLocalizedText(labelResultText, org.openide.util.NbBundle.getMessage(CopyMoveForgeryDetectionDataContentViewer.class, "CopyMoveForgeryDetectionDataContentViewer.labelResultText.text")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(6, 10, 0, 0);
        add(labelResultText, gridBagConstraints);

        panelImage.setBackground(new java.awt.Color(0, 0, 0));

        org.openide.awt.Mnemonics.setLocalizedText(labelResultImage, org.openide.util.NbBundle.getMessage(CopyMoveForgeryDetectionDataContentViewer.class, "CopyMoveForgeryDetectionDataContentViewer.labelResultImage.text")); // NOI18N

        javax.swing.GroupLayout panelImageLayout = new javax.swing.GroupLayout(panelImage);
        panelImage.setLayout(panelImageLayout);
        panelImageLayout.setHorizontalGroup(
            panelImageLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
            .addGroup(panelImageLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(labelResultImage, javax.swing.GroupLayout.DEFAULT_SIZE, 749, Short.MAX_VALUE))
        );
        panelImageLayout.setVerticalGroup(
            panelImageLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 431, Short.MAX_VALUE)
            .addGroup(panelImageLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(panelImageLayout.createSequentialGroup()
                    .addComponent(labelResultImage, javax.swing.GroupLayout.PREFERRED_SIZE, 431, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGap(0, 0, Short.MAX_VALUE)))
        );

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(6, 10, 11, 10);
        add(panelImage, gridBagConstraints);

        jButton1.setBackground(new java.awt.Color(0, 0, 0));
        jButton1.setForeground(new java.awt.Color(255, 255, 255));
        jButton1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/de/fau/copymoveforgerydetection/resources/Save-icon 20.png"))); // NOI18N
        org.openide.awt.Mnemonics.setLocalizedText(jButton1, org.openide.util.NbBundle.getMessage(CopyMoveForgeryDetectionDataContentViewer.class, "CopyMoveForgeryDetectionDataContentViewer.jButton1.text")); // NOI18N
        jButton1.setBorder(null);
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 25, 0, 10);
        add(jButton1, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        add(filler1, gridBagConstraints);
    }// </editor-fold>//GEN-END:initComponents

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int returnVal = fileChooser.showSaveDialog(this);
        
        if(returnVal == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            File outFile = new File(file.getAbsoluteFile(), currentName + "." + currentFileExtension);
            try {
                if (currentImage == null || !ImageIO.write(currentImage, currentFileExtension, outFile)) {
                    logger.log(Level.WARNING, "Nothing written (no image or no writer for format): " + outFile.getAbsolutePath());
                }
            }
            catch (IOException ex) {
                logger.log(Level.SEVERE, "Error when trying to write image to disk (jButton1ActionPerformed): "+outFile.getAbsolutePath(), ex);
            }
        }
    }//GEN-LAST:event_jButton1ActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.Box.Filler filler1;
    private javax.swing.JButton jButton1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JLabel labelResultImage;
    private javax.swing.JLabel labelResultText;
    private javax.swing.JPanel panelImage;
    // End of variables declaration//GEN-END:variables
}
