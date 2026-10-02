package io.github.vgrytsenko2022.plugin;

import io.github.vgrytsenko2022.plugin.options.FlutterOptionsPanelController;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import org.netbeans.api.options.OptionsDisplayer;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;

@ActionID(category = "Flutter", id = "io.github.vgrytsenko2022.plugin.OpenFlutterOptionsAction")
@ActionRegistration(displayName = "Flutter SDK Settings...")
@ActionReference(path = "Menu/Flutter", position = 610)
public final class OpenFlutterOptionsAction implements ActionListener {
    @Override
    public void actionPerformed(ActionEvent event) {
        OptionsDisplayer.getDefault().open(FlutterOptionsPanelController.ID);
    }
}
