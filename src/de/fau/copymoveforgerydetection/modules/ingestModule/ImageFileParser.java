/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.fau.copymoveforgerydetection.modules.ingestModule;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;
import org.sleuthkit.datamodel.AbstractFile;
import org.sleuthkit.datamodel.TskCoreException;
import org.sleuthkit.datamodel.ReadContentInputStream;

/**
 *
 * @author Tobias
 */
public class ImageFileParser {
    
    private static final byte[] pngSignature = { (byte)0x89, (byte)0x50, (byte)0x4E, (byte)0x47, 
                                                 (byte)0x0D, (byte)0x0A, (byte)0x1A, (byte)0x0A };
    private static final byte[] jpgSignature = { (byte)0xFF, (byte)0xD8, (byte)0xFF  };
    private static final byte[] bmpSignature = { (byte)0x42, (byte)0x4D };
    
    public static BufferedImage parseAbstractFile(AbstractFile file) throws IOException{
        InputStream inputStream = new ReadContentInputStream(file);
        return ImageIO.read(inputStream);
    }
    
    /**
     * Determines if the file is a imageFile supported by javas ImageIO or not.
     * 
     * Supported image types are:  JPEG, PNG, GIF, BMP (GIF is not supported by the Fingerprint algorithm though)
     * as well as this wierd bitmap format for small images wbmp that is mostly used in shortmessages 
     * constructing a fingerprint put of those images would make n sense though.
     * 
     * @param file from autopsy
     * @return true if it is a image file supported by ImageIO
     */
    public static boolean isSupportedImageFile(AbstractFile file) {
        // read bytes if unable do not parse
        byte[] signature = new byte[8];
        try {
            file.read(signature, 0, 8);
        } catch (TskCoreException ex) {
            // this exception basically means that the file is shorter than 8 bytes.
            return false;
        }
        
        return compareSignatures(signature,jpgSignature) || 
               compareSignatures(signature,pngSignature) || 
               compareSignatures(signature,bmpSignature);
    }
    
    
    public static String findImageType(AbstractFile file) {
        String imgType = "unsupported";
        
        byte[] signature = new byte[8];
        try {
            file.read(signature, 0, 8);
        } catch (TskCoreException ex) {
            // this exception basically means that the file is shorter than 8 bytes.
            return imgType;
        }
        
        if(compareSignatures(signature,jpgSignature)){
            return "jpg";
        } 
        if(compareSignatures(signature,pngSignature)){
            return "png";
        }
        if(compareSignatures(signature,bmpSignature)){
            return "bmp";
        }
        
        return imgType;
    }
    
    /**
     * Returns true if the first imageSignature.length Bytes of the imported Signature 
     * are the same as the image Signature.
     * 
     * @param signatureFromFile
     * @param imageSignature
     * @return 
     */
    private static boolean compareSignatures(byte[] signatureFromFile, byte[] imageSignature) {
        if(signatureFromFile.length < imageSignature.length)
            return false;
        for(int i = 0; i < imageSignature.length; ++i) {
            if(signatureFromFile[i] != imageSignature[i]) {
                return false;
            }
        }
        return true;
    }
}
