package au.com.codeka.warworlds.model.designeffects;

import org.w3c.dom.Element;

import au.com.codeka.common.design.Design;

public class BuildingEffect extends Design.Effect {
    public BuildingEffect(Element effectElement) {
        super(effectElement.getAttribute("kind"));
    }
}
