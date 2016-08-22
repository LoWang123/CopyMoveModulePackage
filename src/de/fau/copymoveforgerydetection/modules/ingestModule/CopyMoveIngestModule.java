package de.fau.copymoveforgerydetection.modules.ingestModule;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.logging.Level;
import javax.imageio.ImageIO;
import org.sleuthkit.autopsy.casemodule.Case;
import org.sleuthkit.autopsy.coreutils.Logger;
import org.sleuthkit.autopsy.ingest.FileIngestModule;
import org.sleuthkit.autopsy.ingest.IngestModule;
import org.sleuthkit.autopsy.ingest.IngestJobContext;
import org.sleuthkit.autopsy.ingest.IngestServices;
import org.sleuthkit.autopsy.ingest.ModuleDataEvent;
import org.sleuthkit.datamodel.AbstractFile;
import org.sleuthkit.datamodel.BlackboardArtifact;
import org.sleuthkit.datamodel.BlackboardArtifact.ARTIFACT_TYPE;
import org.sleuthkit.datamodel.BlackboardAttribute;
import org.sleuthkit.datamodel.TskCoreException;
import org.sleuthkit.datamodel.SleuthkitCase;
import org.sleuthkit.datamodel.TskData;
import de.fau.copymoveforgerydetection.copymove.RadixSortDetection;
import java.util.ArrayList;
import org.openide.util.Lookup;
import org.sleuthkit.autopsy.ingest.IngestMessage;

/**
 * Sample file ingest module that doesn't do much. Demonstrates per ingest job
 * module settings, use of a subset of the available ingest services and
 * thread-safe sharing of per ingest job data.
 */
class CopyMoveIngestModule implements FileIngestModule {

    private static int attributeIdResultImage = -1;
    private IngestJobContext context = null;
    
    private final int blockSize = 10;    
    private final int regionMinSize;
    
    

    CopyMoveIngestModule(CopyMoveModuleIngestJobSettings settings) {
        this.regionMinSize = settings.getRegionMinSize();;
    }

    @Override
    public void startUp(IngestJobContext context) throws IngestModuleException {
        this.context = context;

        synchronized (CopyMoveIngestModule.class) {
            if (attributeIdResultImage == -1) {
                Case autopsyCase = Case.getCurrentCase();
                SleuthkitCase sleuthkitCase = autopsyCase.getSleuthkitCase();
                try {
                    // See if the attribute type has already been defined.                    
                    attributeIdResultImage = sleuthkitCase.getAttrTypeID(
                            CopyMoveIngestModuleFactory.getResultAttributeTypeString());
                    if (attributeIdResultImage == -1) {
                        attributeIdResultImage = sleuthkitCase.addAttrType(
                                CopyMoveIngestModuleFactory.getResultAttributeTypeString(), 
                                CopyMoveIngestModuleFactory.getResultAttributeDescription());
                    }
                } catch (TskCoreException ex) {
                    IngestServices ingestServices = IngestServices.getInstance();
                    Logger logger = ingestServices.getLogger(CopyMoveIngestModuleFactory.getModuleName());
                    logger.log(Level.SEVERE, "Failed to create blackboard attribute", ex);
                    attributeIdResultImage = -1;
                    throw new IngestModuleException(ex.getLocalizedMessage());
                }
            }
        }
    }

    @Override
    public IngestModule.ProcessResult process(AbstractFile file) {
        if(!isProcessExecutable()) {
            return IngestModule.ProcessResult.ERROR;
        }
        if(!isFileSupported(file)) {
            return IngestModule.ProcessResult.OK;
        }
        try {
            return processCore(file);  
        } catch (TskCoreException ex) {
            IngestServices ingestServices = IngestServices.getInstance();
            Logger logger = ingestServices.getLogger(CopyMoveIngestModuleFactory.getModuleName());
            logger.log(Level.SEVERE, "Error processing file (id = " + file.getId() + ")", ex);
            return IngestModule.ProcessResult.ERROR;
        } catch (IOException ex) {
            IngestServices ingestServices = IngestServices.getInstance();
            Logger logger = ingestServices.getLogger(CopyMoveIngestModuleFactory.getModuleName());
            logger.log(Level.SEVERE, "Error processing file (id = " + file.getId() + ")", ex);
            return IngestModule.ProcessResult.ERROR;
        }  catch (Exception ex) {
            IngestServices ingestServices = IngestServices.getInstance();
            Logger logger = ingestServices.getLogger(CopyMoveIngestModuleFactory.getModuleName());
            logger.log(Level.SEVERE, "Error processing file (id = " + file.getId() + ")", ex);
            return IngestModule.ProcessResult.ERROR;
        } 
    }

    private boolean isProcessExecutable() {
        if (attributeIdResultImage == -1) {
            return false;
        }
        return true;
    }
    
    private boolean isFileSupported(AbstractFile file) {
         if ((file.getType() == TskData.TSK_DB_FILES_TYPE_ENUM.UNALLOC_BLOCKS)
                || (file.getType() == TskData.TSK_DB_FILES_TYPE_ENUM.UNUSED_BLOCKS)
                || (file.isFile() == false)
                || !ImageFileParser.isSupportedImageFile(file)) {
            return false;
        }
        return true;
    }
    
    private IngestModule.ProcessResult processCore(AbstractFile file) 
            throws IOException, TskCoreException {
        BufferedImage image = ImageFileParser.parseAbstractFile(file);
        RadixSortDetection rsd = new RadixSortDetection();
        rsd.setBlockSideLength(this.blockSize);
        rsd.setRegionMinSize(this.regionMinSize);
        rsd.setParallelize(false);
        rsd.setInputImage(image);
        rsd.run();
        
        BufferedImage resultImage = rsd.getResultImage();
        int numberOfCopiedRegions = rsd.getResultList().size();
        
        
        submitResult(file, image, resultImage);
        reportBlackboardCopiedRegionsCount(file.getName(), numberOfCopiedRegions);
        
        return IngestModule.ProcessResult.OK;
    }
    
    @Override
    public void shutDown() {
    }
    
    private void submitResult(
            AbstractFile file,
            BufferedImage originalImage,
            BufferedImage resultImage)  throws TskCoreException, IOException {
        
            
            
            byte[] resultImageBytes = convertImageToByteArray(resultImage, ImageFileParser.findImageType(file));
            ArrayList<BlackboardAttribute> attributes = new ArrayList<BlackboardAttribute>();
            attributes.add(new BlackboardAttribute(
                        BlackboardAttribute.ATTRIBUTE_TYPE.TSK_VALUE.getTypeID(),
                        CopyMoveIngestModuleFactory.getModuleName(),
                        resultImageBytes));
            
            attributes.add(new BlackboardAttribute(
                        BlackboardAttribute.ATTRIBUTE_TYPE.TSK_COMMENT.getTypeID(), 
                        CopyMoveIngestModuleFactory.getModuleName(),
                        file.getName() + "_CopyMoveResult." + file.getNameExtension())); 
            
            BlackboardArtifact artifactIFH = file.newArtifact(BlackboardArtifact.ARTIFACT_TYPE.TSK_INTERESTING_FILE_HIT);
            artifactIFH.addAttributes(attributes);
            
            ModuleDataEvent event = new ModuleDataEvent(CopyMoveIngestModuleFactory.getModuleName(), ARTIFACT_TYPE.TSK_INTERESTING_FILE_HIT);
            IngestServices.getInstance().fireModuleDataEvent(event);
    }
    
    private byte[] convertImageToByteArray(BufferedImage image, String extension)
            throws IOException{
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, extension, baos);
        return baos.toByteArray();
    }

   
    synchronized static void reportBlackboardCopiedRegionsCount(String imageName, int numberOfCopiedRegions) {
        String messageString = "";
        
        if(numberOfCopiedRegions == 1) {
            messageString = "Found " + numberOfCopiedRegions + " copied region in " + imageName;
        }
        else {
            messageString = "Found " + numberOfCopiedRegions + " copied regions in " + imageName;
        }
        
        IngestMessage message = IngestMessage.createMessage(
                IngestMessage.MessageType.INFO,
                CopyMoveIngestModuleFactory.getModuleName(),
                messageString);
        IngestServices.getInstance().postMessage(message);
    }
}
