package com.github.rskupnik;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.rskupnik.parrot.Parrot;
import com.tulskiy.keymaster.common.Provider;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import sx.blah.discord.api.ClientBuilder;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.ReadyEvent;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.util.DiscordException;
import sx.blah.discord.util.MissingPermissionsException;
import sx.blah.discord.util.RateLimitException;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Optional;

/**
 * @author Radoslaw Skupnik
 * @since 2016-10-02
 */
public class Snapcord {

    private Parrot parrot;  // Parrot is a wrapper for properties file
    private IDiscordClient api;

    private boolean ready;

    // Properties
    private String email, password, hotkey, channelName, channelId;

    public Snapcord() {
        try {
            printLogo();
            loadProperties();
            setupGlobalHotkey();
            connect();
        } catch(DiscordException e) {
            e.printStackTrace();
            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
        }
    }

    private void printLogo() {
        System.out.println(" _____                                     _ ");
        System.out.println("/  ___|                                   | |");
        System.out.println("\\ `--. _ __   __ _ _ __   ___ ___  _ __ __| |");
        System.out.println(" `--. \\ '_ \\ / _` | '_ \\ / __/ _ \\| '__/ _` |");
        System.out.println("/\\__/ / | | | (_| | |_) | (_| (_) | | | (_| |");
        System.out.println("\\____/|_| |_|\\__,_| .__/ \\___\\___/|_|  \\__,_|");
        System.out.println("                  | |                        ");
        System.out.println("                  |_|                        ");
        System.out.println("by Myzreal");
        System.out.println("");
        System.out.println("To exit, press Ctrl+C");
        System.out.println();
    }

    private void loadProperties() {
        parrot = new Parrot();
        email = parrot.get("email").orElseThrow(() -> new RuntimeException("Email not provided"));
        password = parrot.get("password").orElseThrow(() -> new RuntimeException("Password not provided"));
        hotkey = parrot.get("hotkey").orElseThrow(() -> new RuntimeException("Hotkey not provided"));
        channelId = parrot.get("channel_id").orElse(null);
        if (StringUtils.isBlank(channelId)) {
            channelName = parrot.get("channel_name").orElseThrow(() -> new RuntimeException("No channel_id nor channel_name provided"));
        }
    }

    private void setupGlobalHotkey() {
        Provider provider = Provider.getCurrentProvider(false);
        provider.register(KeyStroke.getKeyStroke(hotkey), hotKey -> {
            if (!ready)
                return;

            System.out.println("Snap! Hope you don't have anything inappropriate open :)");

            Optional<File> file = captureScreenshot();
            if (!file.isPresent())
                return;

            Optional<Response> response = postScreenshot(file.get());
            if (!response.isPresent())
                return;

            Optional<IChannel> channel = getChannel();
            if (!channel.isPresent())
                return;

            try {
                channel.get().sendMessage(response.get().getImage());
            } catch (MissingPermissionsException | RateLimitException | DiscordException e) {
                e.printStackTrace();
            }
        });
    }

    private void connect() throws DiscordException {
        System.out.println("Connecting, please wait...");
        ClientBuilder clientBuilder = new ClientBuilder();
        clientBuilder.withLogin(email, password);
        api = clientBuilder.login();
        api.getDispatcher().registerListener(this);
    }

    /**
     * Retrieves the channel that the image link should be posted to.
     * If channel_id is provided, it uses that to get the channel.
     * If not - it uses channel_name to find the channel.
     */
    private Optional<IChannel> getChannel() {
        if (StringUtils.isNotBlank(channelId)) {
            return Optional.of(api.getChannelByID(channelId));
        } else {
            if (StringUtils.isBlank(channelName))
                return Optional.empty();

            for (IChannel channel : api.getChannels(false)) {
                if (channel.getName().equals(channelName))
                    return Optional.of(channel);
            }
        }

        return Optional.empty();
    }

    @EventSubscriber
    public void onReadyEvent(ReadyEvent event) {
        System.out.println("Ready! - use ["+hotkey+"] to take a screenshot and post it to your discord channel");
        ready = true;
    }

    /**
     * Captures a screenshot and saves it to a screenshot.jpg
     * file in the root folder. Returns an optional File object pointing
     * to this screenshot.
     */
    private Optional<File> captureScreenshot() {
        try {
            Robot robot = new Robot();
            Rectangle screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
            BufferedImage img = robot.createScreenCapture(screenRect);
            File file = new File("screenshot.jpg");
            ImageIO.write(img, "jpg", file);
            return Optional.of(file);
        } catch (AWTException | IOException e) {
            e.printStackTrace();
        }

        return Optional.empty();
    }

    /**
     * Posts the provided file to a image hosting service.
     * The file has to be a jpeg image.
     */
    private Optional<Response> postScreenshot(File file) {
        try {
            HttpPost uploadFile = new HttpPost("https://vgy.me/upload");
            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            builder.addBinaryBody("file", file, ContentType.create("image/jpeg"), file.getName());
            HttpEntity multipart = builder.build();
            uploadFile.setEntity(multipart);

            CloseableHttpResponse response = HttpClients.createDefault().execute(uploadFile);

            HttpEntity responseEntity = response.getEntity();
            String body = EntityUtils.toString(responseEntity, "UTF-8");
            ObjectMapper objectMapper = new ObjectMapper();
            Response output = objectMapper.readValue(body, Response.class);

            return Optional.of(output);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return Optional.empty();
    }

    public static void main(String[] args) {
        new Snapcord();
    }
}
