package io.sked.docs.core.listener.async;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;
import io.sked.docs.core.dao.FileDao;
import io.sked.docs.core.dao.UserDao;
import io.sked.docs.core.event.FileCreatedAsyncEvent;
import io.sked.docs.core.event.FileEvent;
import io.sked.docs.core.event.FileUpdatedAsyncEvent;
import io.sked.docs.core.model.context.AppContext;
import io.sked.docs.core.model.jpa.File;
import io.sked.docs.core.model.jpa.User;
import io.sked.docs.core.util.DirectoryUtil;
import io.sked.docs.core.util.EncryptionUtil;
import io.sked.docs.core.util.FileUtil;
import io.sked.docs.core.util.TransactionUtil;
import io.sked.docs.core.util.format.FormatHandler;
import io.sked.docs.core.util.format.FormatHandlerUtil;
import io.sked.util.ImageUtil;
import io.sked.util.Scalr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import java.awt.image.BufferedImage;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Listener on file processing.
 * 
 * @author bgamard
 */
public class FileProcessingAsyncListener {
    /**
     * Logger.
     */
    private static final Logger log = LoggerFactory.getLogger(FileProcessingAsyncListener.class);

    /**
     * File created.
     *
     * @param event File created event
     */
    @Subscribe
    @AllowConcurrentEvents
    public void on(final FileCreatedAsyncEvent event) {
        if (log.isInfoEnabled()) {
            log.info("File created event: " + event.toString());
        }

        processFile(event, true);
    }

    /**
     * File updated.
     *
     * @param event File updated event
     */
    @Subscribe
    @AllowConcurrentEvents
    public void on(final FileUpdatedAsyncEvent event) {
        log.info("File updated event: " + event.toString());

        processFile(event, false);
    }

    /**
     * Process a file :
     * Generate thumbnails
     * Extract and save text content
     *
     * @param event File event
     * @param isFileCreated True if the file was just created
     */
    private void processFile(FileEvent event, boolean isFileCreated) {
        AtomicReference<File> file = new AtomicReference<>();
        AtomicReference<User> user = new AtomicReference<>();

        // Open a first transaction to get what we need to start the processing
        TransactionUtil.handle(() -> {
            // Generate thumbnail, extract content
            file.set(new FileDao().getActiveById(event.getFileId()));
            if (file.get() == null) {
                // The file has been deleted since
                return;
            }

            // Get the creating user from the database for its private key
            UserDao userDao = new UserDao();
            user.set(userDao.getById(file.get().getUserId()));
        });

        // Process the file outside of a transaction
        if (user.get() == null || file.get() == null) {
            // The user or file has been deleted
            FileUtil.endProcessingFile(event.getFileId());
            return;
        }
        String content = extractContent(event, user.get(), file.get());

        // Open a new transaction to save the file content
        TransactionUtil.handle(() -> {
            // Save the file to database
            FileDao fileDao = new FileDao();
            File freshFile = fileDao.getActiveById(event.getFileId());
            if (freshFile == null) {
                // The file has been deleted since the text extraction started, ignore the result
                return;
            }

            freshFile.setContent(content);
            fileDao.updateContent(freshFile);

            // Update index with the updated file
            if (isFileCreated) {
                AppContext.getInstance().getIndexingHandler().createFile(freshFile);
            } else {
                AppContext.getInstance().getIndexingHandler().updateFile(freshFile);
            }
        });

        FileUtil.endProcessingFile(event.getFileId());
    }

    /**
     * Extract text content from a file.
     * This is executed outside of a transaction.
     *
     * @param event File event
     * @param user User whom created the file
     * @param file Fresh file
     * @return Text content
     */
    private String extractContent(FileEvent event, User user, File file) {
        // Find a format handler
        FormatHandler formatHandler = FormatHandlerUtil.find(file.getMimeType());
        if (formatHandler == null) {
            log.info("Format unhandled: " + file.getMimeType());
            return null;
        }

        // Generate file variations
        try {
            Cipher cipher = EncryptionUtil.getEncryptionCipher(user.getPrivateKey());
            BufferedImage image = formatHandler.generateThumbnail(event.getUnencryptedFile());
            if (image != null) {
                // Generate thumbnails from image
                BufferedImage web = Scalr.resize(image, Scalr.Method.ULTRA_QUALITY, Scalr.Mode.AUTOMATIC, 1280);
                BufferedImage thumbnail = Scalr.resize(image, Scalr.Method.ULTRA_QUALITY, Scalr.Mode.AUTOMATIC, 256);
                image.flush();

                // Write "web" encrypted image
                Path outputFile = DirectoryUtil.getStorageDirectory().resolve(file.getId() + "_web");
                try (OutputStream outputStream = new CipherOutputStream(Files.newOutputStream(outputFile), cipher)) {
                    ImageUtil.writeJpeg(web, outputStream);
                }

                // Write "thumb" encrypted image
                outputFile = DirectoryUtil.getStorageDirectory().resolve(file.getId() + "_thumb");
                try (OutputStream outputStream = new CipherOutputStream(Files.newOutputStream(outputFile), cipher)) {
                    ImageUtil.writeJpeg(thumbnail, outputStream);
                }
            }
        } catch (Throwable e) {
            log.error("Unable to generate thumbnails for: " + file, e);
        }

        // Extract text content from the file
        long startTime = System.currentTimeMillis();
        String content = null;
        log.info("Start extracting content from: " + file);
        try {
            content = formatHandler.extractContent(event.getLanguage(), event.getUnencryptedFile());
        } catch (Throwable e) {
            log.error("Error extracting content from: " + file, e);
        }
        log.info(MessageFormat.format("File content extracted in {0}ms: " + file.getId(), System.currentTimeMillis() - startTime));

        return content;
    }
}
