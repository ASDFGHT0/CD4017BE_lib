package cd4017be.lib.render.model;

import com.google.common.base.Optional;

import cd4017be.lib.script.Parameters;
import cd4017be.lib.util.Orientation;
import net.minecraft.client.renderer.block.model.ModelRotation;
import net.minecraftforge.common.model.IModelPart;
import net.minecraftforge.common.model.IModelState;
import net.minecraftforge.common.model.TRSRTransformation;

/**
 * 
 * @author cd4017be
 */
public class ParamertisedVariant implements IModelState {

	public static final ParamertisedVariant BASE = new ParamertisedVariant("model", null);

	public final ModelRotation orient;
	public String subModel;
	public final Parameters params;

	public ParamertisedVariant(ModelRotation orient, String name, Parameters param) {
		this.orient = orient;
		this.subModel = name;
		this.params = param;
	}

	public ParamertisedVariant(String name, Parameters param) {this(ModelRotation.X0_Y0, name, param);}
	public ParamertisedVariant(ModelRotation orient) {this(orient, "model", null);}

	public String splitPath() {
		String[] parts = subModel.split("\\.");
		String path = parts[0];
		if (parts.length > 1) path += "." + parts[1];
		if (parts.length > 2) subModel = parts[2];
		else subModel = "model";
		return path;
	}

	public boolean isBase() {
		return this == BASE || params == null && orient == ModelRotation.X0_Y0 && subModel.equals(BASE.subModel);
	}

	@Override
	public Optional<TRSRTransformation> apply(Optional<? extends IModelPart> part) {
		return orient.apply(part);
	}

	public static ParamertisedVariant parse(String name, ModelRotation orient) {
		int i = name.indexOf('(');
		Parameters params;
		if (i < 0) params = null;
		else {
			String arg = name.substring(i + 1, name.length() - (name.endsWith(")") ? 1 : 0));
			name = name.substring(0, i);
			if (arg.isEmpty()) params = new Parameters();
			else {
				String[] pars = arg.split(",");
				Object[] arr = new Object[pars.length];
				for (int j = 0; j < pars.length; j++)
					try {
						arr[j] = Double.parseDouble(pars[j]);
					} catch (NumberFormatException e) {
						arr[j] = pars[j];
					}
				params = new Parameters(arr);
			}
		}
		return new ParamertisedVariant(orient, name, params);
	}

	public static ParamertisedVariant parse(String name) {
		int i = name.indexOf('#');
		ModelRotation orient;
		if (i < 0) orient = ModelRotation.X0_Y0;
		else {
			Orientation or = Orientation.valueOf(name.substring(i + 1));
			orient = or == null ? ModelRotation.X0_Y0 : or.getModelRotation();
			name = name.substring(0, i);
		}
		return parse(name, orient);
	}

}
