package com.github.rskupnik;

import com.google.common.util.concurrent.FutureCallback;
import de.btobastian.javacord.DiscordAPI;
import de.btobastian.javacord.Javacord;
import de.btobastian.javacord.listener.message.MessageCreateListener;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.codehaus.jackson.map.ObjectMapper;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class Snapper {

    private DiscordAPI discordAPI;

    public Snapper() {
        discordAPI = Javacord.getApi("MjMxNzU1MDgwMTU4ODA2MDE2.CtE-aw.SXSzdjJvbFdP9XX0mg5vCl6OKtU", true); // TODO: Load token from config or hardcode
        discordAPI.connect(new FutureCallback<DiscordAPI>() {
            @Override
            public void onSuccess(DiscordAPI discordAPI) {
                discordAPI.registerListener((MessageCreateListener) (discordAPI1, message) -> {
                    if (message.getContent().equalsIgnoreCase("ping")) {
                        message.reply("pong");
                    } else if (message.getContent().equalsIgnoreCase("!screenshot")) {
                        message.reply("Processing, please wait...");
                        File file = captureScreenshot();
                        if (file != null) {
                            Response response = postScreenshot(file);
                            if (response != null && !response.isError()) {
                                System.out.println(response.getImage());
                                message.reply(response.getImage());
                            }
                        }
                    }
                });
            }

            @Override
            public void onFailure(Throwable throwable) {
                throwable.printStackTrace();
            }
        });
    }

    private File captureScreenshot() {
        try {
            Robot robot = new Robot();
            Rectangle screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
            BufferedImage img = robot.createScreenCapture(screenRect);
            File file = new File("screenshot.jpg");
            ImageIO.write(img, "jpg", file);
            return file;
        } catch (AWTException | IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    private Response postScreenshot(File file) {
        try {
            CloseableHttpClient httpClient = HttpClients.createDefault();
            HttpPost uploadFile = new HttpPost("https://vgy.me/upload");
            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            builder.addBinaryBody("file", file, ContentType.create("image/jpeg"), file.getName());
            HttpEntity multipart = builder.build();
            uploadFile.setEntity(multipart);
            CloseableHttpResponse response = httpClient.execute(uploadFile);
            HttpEntity responseEntity = response.getEntity();
            String body = EntityUtils.toString(responseEntity, "UTF-8");
            ObjectMapper objectMapper = new ObjectMapper();
            Response output = null;
            try {
                output = objectMapper.readValue(body, Response.class);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return output;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static void main( String[] args ) {
        new Snapper();
    }
}
