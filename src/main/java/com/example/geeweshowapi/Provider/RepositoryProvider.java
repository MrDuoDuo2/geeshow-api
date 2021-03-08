package com.example.geeweshowapi.Provider;

import com.example.geeweshowapi.controller.MysqlController;
import com.example.geeweshowapi.model.User;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;

@Component
public class RepositoryProvider {

    @Value("${git.path}")
    public String git_path;

    MysqlController mysqlController = new MysqlController();

    public String addUserRepository(String user_id) throws IOException {
        mysqlController.init();
        String user_repository_path;

        try {
            User user = mysqlController.findByUserId(user_id);
            user_repository_path = user.getRepositoryPath();
            File file = new File(user_repository_path);
            if (!file.exists()) {
                file.mkdir();
            }

        } catch (NullPointerException e) {
            File file = new File(String.format("%s/%s", git_path, user_id));
            if (!file.exists()) {
                file.mkdir();
            }
            user_repository_path = String.format("%s/%s", git_path, user_id);
        }

        return user_repository_path;
    }
}
