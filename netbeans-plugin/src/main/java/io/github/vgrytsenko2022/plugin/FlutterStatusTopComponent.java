package io.github.vgrytsenko2022.plugin;

import java.awt.BorderLayout;
import javax.swing.*;
import org.openide.windows.TopComponent;
import org.openide.util.NbBundle.Messages;

@TopComponent.Description(preferredID = "FlutterStatusTopComponent", persistenceType = TopComponent.PERSISTENCE_ALWAYS)
@TopComponent.Registration(mode = "output", openAtStartup = false, position = 3500)
@TopComponent.OpenActionRegistration(displayName = "Flutter", preferredID = "FlutterStatusTopComponent")
@Messages({
    "CTL_FlutterStatusTopComponent=Flutter",
    "TXT_FlutterStatusTopComponent=Flutter and Dart SDK settings are available in Tools > Options > Flutter.\n\nCreate an application with File > New Project > Flutter, or open an existing Flutter directory with File > Open Project. The Run toolbar automatically tracks connected Desktop, Mobile, and Web targets. Use the standard Run menu for Build, Clean, or Clean and Build. Use the Flutter menu to launch a configured mobile emulator, Run or Debug, Hot Reload, Hot Restart, or Stop."
})
public final class FlutterStatusTopComponent extends TopComponent {
    private final JTextArea text = new JTextArea();
    public FlutterStatusTopComponent() {
        setName(Bundle.CTL_FlutterStatusTopComponent());
        setLayout(new BorderLayout());
        text.setEditable(false);
        text.setText(Bundle.TXT_FlutterStatusTopComponent());
        add(new JScrollPane(text), BorderLayout.CENTER);
    }
}
