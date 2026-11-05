package com.example.orderintegration.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

import org.apache.sshd.common.file.virtualfs.VirtualFileSystemFactory;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.auth.password.PasswordAuthenticator;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.sftp.server.SftpSubsystemFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EmbeddedSftpServer {

    private static final int SFTP_PORT = 2222;

    private static final Path SFTP_ROOT =
            Path.of("C:/CamelProject/sftp-server");

    @Bean(initMethod = "start", destroyMethod = "stop")
    public SshServer sftpServer() throws Exception {

        Files.createDirectories(SFTP_ROOT);
        Files.createDirectories(SFTP_ROOT.resolve("outbound"));

        SshServer server = SshServer.setUpDefaultServer();

        server.setPort(SFTP_PORT);

        server.setKeyPairProvider(
                new SimpleGeneratorHostKeyProvider(
                        Path.of("sftp-hostkey.ser")));

        PasswordAuthenticator authenticator =
                (username, password, session) ->
                        "cameluser".equals(username)
                        && "camel123".equals(password);

        server.setPasswordAuthenticator(authenticator);

        server.setSubsystemFactories(
                Collections.singletonList(
                        new SftpSubsystemFactory.Builder().build()));

        server.setFileSystemFactory(
                new VirtualFileSystemFactory(SFTP_ROOT));

        return server;
    }
}
