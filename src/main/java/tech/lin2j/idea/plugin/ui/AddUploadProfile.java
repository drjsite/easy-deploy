package tech.lin2j.idea.plugin.ui;

import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.vfs.VirtualFile;
import org.jdesktop.swingx.prompt.PromptSupport;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tech.lin2j.idea.plugin.domain.model.Command;
import tech.lin2j.idea.plugin.domain.model.ConfigHelper;
import tech.lin2j.idea.plugin.domain.model.NoneCommand;
import tech.lin2j.idea.plugin.domain.model.UploadProfile;
import tech.lin2j.idea.plugin.domain.model.event.UploadProfileAddEvent;
import tech.lin2j.idea.plugin.domain.model.event.UploadProfileSelectedEvent;
import tech.lin2j.idea.plugin.event.ApplicationContext;
import tech.lin2j.idea.plugin.uitl.FileUtil;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.io.File;
import java.util.Objects;

/**
 * @author linjinjia
 * @date 2022/5/24 15:45
 */
public class AddUploadProfile extends DialogWrapper {
    private JTextField nameInput;
    private JComboBox<Command> commandBox;
    private JTextField fileInput;
    private JButton cancelBtn;
    private JButton okBtn;
    private JButton browserButton;
    private JTextField locationInput;
    private JPanel mainPanel;
    private JTextField excludeInput;

    private final Integer sshId;
    private final UploadProfile profile;

    public AddUploadProfile(Integer sshId, UploadProfile profile) {
        super(true);
        this.sshId = sshId;
        this.profile = profile;

        initUi();
        setTitle("Add Upload Profile");
        init();
    }

    public void initUi() {
        Toolkit tk = Toolkit.getDefaultToolkit();
        mainPanel.setMinimumSize(new Dimension(tk.getScreenSize().width / 2, 0));

        commandBox.addItem(NoneCommand.INSTANCE);
        ConfigHelper.getCommandsBySshId(sshId).forEach(cmd -> commandBox.addItem(cmd));

        excludeInput.setEnabled(false);
        PromptSupport.setPrompt("*.log;*.iml", excludeInput);

        if (profile != null) {
            nameInput.setText(profile.getName());
            fileInput.setText(profile.getFile());
            if (FileUtil.isDirectory(profile.getFile())) {
                excludeInput.setEnabled(true);
                excludeInput.setText(profile.getExclude());
            }
            locationInput.setText(profile.getLocation());
            for (int i = 0; i < commandBox.getItemCount(); i++) {
                Command command = commandBox.getItemAt(i);
                if (Objects.equals(command.getId(), profile.getCommandId())) {
                    commandBox.setSelectedIndex(i);
                }
            }
        }

        fileInput.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
            }
            @Override
            public void focusLost(FocusEvent e) {
                excludeInput.setEnabled(FileUtil.isDirectory(fileInput.getText()));
            }
        });

        browserButton.addActionListener(e -> {
            Project project = ProjectManager.getInstance().getDefaultProject();
            FileChooserDescriptor chooserDescriptor = new FileChooserDescriptor(true, true, true, true, true, false);
            VirtualFile virtualFile = FileChooser.chooseFile(chooserDescriptor, project, null);
            if (virtualFile != null) {
                fileInput.setText(virtualFile.getPath());
                excludeInput.setEnabled(virtualFile.isDirectory());
            }
        });

        okBtn.addActionListener(new OkListener());

        cancelBtn.addActionListener(e -> {
            close(OK_EXIT_CODE);
        });
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        JPanel dialog = new JPanel(new BorderLayout());
        dialog.add(mainPanel, BorderLayout.CENTER);
        return dialog;
    }

    @Override
    protected Action @NotNull [] createActions() {
        return new Action[]{};
    }

    private class OkListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            String name = nameInput.getText();
            String file = fileInput.getText();
            String location = locationInput.getText();
            Command command = (Command) commandBox.getSelectedItem();

            String exclude = excludeInput.getText();
            if (!FileUtil.isDirectory(file)) {
                exclude = "";
            }

            // update config if profile is exist
            if (profile != null) {
                profile.setName(name);
                profile.setSshId(sshId);
                profile.setFile(file);
                profile.setExclude(exclude);
                profile.setLocation(location);
                profile.setCommandId(command == NoneCommand.INSTANCE ? null : command.getId());
                profile.setSelected(true);
                ApplicationContext.getApplicationContext().publishEvent(new UploadProfileSelectedEvent(profile));
            } else {
                UploadProfile newProfile = new UploadProfile();
                newProfile.setName(name);
                newProfile.setSshId(sshId);
                newProfile.setFile(file);
                newProfile.setExclude(exclude);
                newProfile.setLocation(location);
                newProfile.setCommandId(command == NoneCommand.INSTANCE ? null : command.getId());
                newProfile.setSelected(true);

                ConfigHelper.addUploadProfile(newProfile);
                ApplicationContext.getApplicationContext().publishEvent(new UploadProfileSelectedEvent(newProfile));
            }

            ApplicationContext.getApplicationContext().publishEvent(new UploadProfileAddEvent());
            close(OK_EXIT_CODE);
        }
    }
}