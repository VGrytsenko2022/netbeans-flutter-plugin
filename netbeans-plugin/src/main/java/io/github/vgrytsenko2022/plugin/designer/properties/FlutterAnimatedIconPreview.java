package io.github.vgrytsenko2022.plugin.designer.properties;

import io.github.vgrytsenko2022.designer.catalog.*;
import io.github.vgrytsenko2022.designer.model.PropertyValue;
import java.awt.*;
import javax.swing.*;
import org.openide.util.ImageUtilities;

/** Reviewed SDK-derived SVG frames, never font glyph approximations. */
final class FlutterAnimatedIconPreview {
    static final String ROOT="io/github/vgrytsenko2022/plugin/designer/icons/animated/";
    static final String PREVIEW_NAME="flutter.animatedIcon.preview";
    private FlutterAnimatedIconPreview(){}
    static boolean supports(FlutterTypedPropertyEditors.Binding binding){
        return binding.definition().constraints().stream().anyMatch(c->c instanceof PropertyValueConstraint.DartObjectReferenceValues r && r.expectedDartType().equals("AnimatedIconData"));
    }
    static Icon icon(String name,boolean dark){
        if(!AnimatedIconWidgetPropertySchema.ICONS.contains(name))return null;
        return ImageUtilities.loadImageIcon(ROOT+name+(dark?"_dark":"")+".svg",false);
    }
    private static boolean lightForeground(Color color){return color!=null && color.getRed()+color.getGreen()+color.getBlue()>510;}
    static void decorate(JLabel label,String name){
        label.setIcon(icon(name,lightForeground(label.getForeground())));
        label.setText("AnimatedIcons."+name);
        label.setToolTipText("Frames at 0%, 50%, 100%"+(label.getIcon()==null?"; SVG preview unavailable":""));
        label.getAccessibleContext().setAccessibleName("AnimatedIcons."+name+"; frames at 0, 50 and 100 percent");
    }
    static void paint(Graphics graphics,Rectangle bounds,FlutterPropertyCellValue value,String text){
        int offset=2;
        if(value.explicitValue().orElse(null) instanceof PropertyValue.StringValue preset){
            Icon icon=icon(preset.value(),lightForeground(graphics.getColor()));
            if(icon!=null && bounds.height>0){
                Graphics2D g=(Graphics2D)graphics.create();
                try{
                    double scale=Math.max(0,Math.min(1,(bounds.height-2)/32.0));
                    g.clip(bounds);g.translate(bounds.x+2,bounds.y+1);g.scale(scale,scale);
                    icon.paintIcon(null,g,0,0);offset+=(int)Math.ceil(icon.getIconWidth()*scale)+5;
                }finally{g.dispose();}
            }
        }
        Graphics2D g=(Graphics2D)graphics.create();
        try{g.clip(bounds);var fm=g.getFontMetrics();g.drawString(text,bounds.x+offset,bounds.y+(bounds.height+fm.getAscent()-fm.getDescent())/2);}finally{g.dispose();}
    }
}
