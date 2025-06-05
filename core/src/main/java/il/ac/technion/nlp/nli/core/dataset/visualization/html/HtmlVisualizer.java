package il.ac.technion.nlp.nli.core.dataset.visualization.html;

import ofergivoli.olib.general.CommandRunner;
import ofergivoli.olib.io.GeneralFileUtils;
import ofergivoli.olib.io.TextIO;
import ofergivoli.olib.io.binary_files.PngFileInMemo;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.RandomStringUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * @author Ofer Givoli <ogivoli@cs.technion.ac.il>
 */
public class HtmlVisualizer {

    /**
     * @param tempDirectory a temporary directory will be created under this directory.
     */
    public PngFileInMemo createImageFromHtml(HtmlString htmlString, Path tempDirectory,
                                             Path wkhtmltoimageAppExecutable) {



        Path tempSubdir;
        try {
            tempSubdir = Files.createTempDirectory(tempDirectory, null);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        File tmpHtml = new File(tempSubdir.toFile(), RandomStringUtils.randomAlphanumeric(10) + ".html");
        File tmpImage = new File(tempSubdir.toFile(), RandomStringUtils.randomAlphanumeric(10) + ".png");
        File tmpImageAfterShrinking = new File(tempSubdir.toFile(), RandomStringUtils.randomAlphanumeric(10)
                + ".png");

        TextIO.writeTextToFileInStandardEncoding(tmpHtml,htmlString.getString(),true);



        new CommandRunner().runAndBlock(true, false, null, null, wkhtmltoimageAppExecutable.toString(), "--quality", "100",
                tmpHtml.getAbsolutePath(), tmpImage.getAbsolutePath());


        shrinkImageFile(tmpImage,tmpImageAfterShrinking);
        PngFileInMemo png = new PngFileInMemo(tmpImageAfterShrinking);


        GeneralFileUtils.deleteAndKeepTryingUntilSuccessful(tempSubdir,100);

        return png;

    }




    /**
     * Reads an image from file and then writes the same image to another file - hopefully smaller file
     * size.
     * @param inImage The extension defines the input file format.
     * @param outImage The extension defines the input file format.
     */
    private void shrinkImageFile(File inImage, File outImage) {
        /*
          Based on: [http://www.mkyong.com/java/convert-png-to-jpeg-image-file-in-java/]
         */

        BufferedImage bufferedImage;
        try {
            bufferedImage = ImageIO.read(inImage);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        BufferedImage newBufferedImage = new BufferedImage(bufferedImage.getWidth(),
                bufferedImage.getHeight(), BufferedImage.TYPE_INT_RGB);

        newBufferedImage.createGraphics().drawImage(bufferedImage, 0, 0, Color.WHITE, null);

        try {
            ImageIO.write(newBufferedImage, FilenameUtils.getExtension(outImage.getAbsolutePath()) , outImage);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

}
