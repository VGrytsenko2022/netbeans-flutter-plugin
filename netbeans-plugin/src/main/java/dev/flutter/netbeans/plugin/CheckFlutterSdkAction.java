package dev.flutter.netbeans.plugin;

import dev.flutter.netbeans.plugin.settings.FlutterToolchainService;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JOptionPane;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;

@ActionID(category = "Tools", id = "dev.flutter.netbeans.plugin.CheckFlutterSdkAction")
@ActionRegistration(displayName = "Check Flutter and Dart SDKs")
@ActionReferences({
    @ActionReference(path = "Menu/Tools", position = 1450),
    @ActionReference(path = "Menu/Flutter", position = 600, separatorBefore = 550)
})
public final class CheckFlutterSdkAction implements ActionListener {
    @Override public void actionPerformed(ActionEvent e) {
        var status = new FlutterToolchainService().resolve();
        String message = status.flutterMessage() + "\n\n" + status.dartMessage();
        if (!status.isReady()) {
            message += "\n\nConfigure the missing SDK in Tools > Options > Flutter.";
        }
        JOptionPane.showMessageDialog(null,
                message,
                "Flutter and Dart SDKs",
                status.isReady() ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.WARNING_MESSAGE);
    }
}
