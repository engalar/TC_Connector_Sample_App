// @<COPYRIGHT>@
// ==================================================
// Copyright 2019
// Siemens Product Lifecycle Management Software Inc.
// All Rights Reserved.
// ==================================================
// @<COPYRIGHT>@

package tcconnector.internal.foundation;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import com.mendix.core.Core;
import com.mendix.systemwideinterfaces.core.IContext;
import com.mendix.systemwideinterfaces.core.IMendixObject;
import com.mendix.systemwideinterfaces.core.meta.IMetaAssociation;

public class OperationSubstitution {
	private IContext context;
	private IMendixObject rootEntity;
	private String path;
	private String attributeName;
	private Map<String, Object> instructions;

	public OperationSubstitution(IContext context, String replacement, IMendixObject srcEntity) {
		this.context = context;
		this.rootEntity = srcEntity;
		this.path = getPath(replacement);
		this.attributeName = parseAttribute();
		this.instructions = parseInstructions();
	}

	public String getAttributeName() {
		return attributeName;
	}

	public Map<String, Object> getInstructions() {
		return instructions;
	}

	public String getAssociationName() {
		return parseAssociation();
	}

	public List<IMendixObject> getLeafEntity() {
		return getLeaf();
	}

	public IMendixObject createLeafEntity() {
		return createLeaf();
	}

	public IMendixObject createLeafParentEntity() {
		return createLeafParent();
	}

	private String getPath(String replacement) {
		int slashIndex = replacement.indexOf('/');
		int semiIndex = replacement.indexOf(';');
		if (slashIndex > 0 && (slashIndex < semiIndex || semiIndex < 0))
			return replacement.substring(slashIndex);
		if (semiIndex > 0)
			return replacement.substring(semiIndex);
		return "";
	}

	private List<IMendixObject> getLeaf() {
		String[] elements = path.split(";")[0].split("\\/");
		IMendixObject leaf = rootEntity;
		List<IMendixObject> childEntities = new ArrayList<IMendixObject>();

		for (int i = 1; i < elements.length; i++) {
			String memberName = elements[i];
			if (!memberName.contains("."))
				break;

			childEntities = Core.retrieveByPath(context, leaf, memberName, true);
			if (childEntities.size() == 0) {
				throw new IllegalArgumentException(
						leaf.getMetaObject().getName() + " does not have a value for " + memberName + ".");
			}
		}

		if (childEntities.size() == 0) {
			childEntities.add(leaf);
		}
		return childEntities;
	}

	private IMendixObject createLeaf() {
		String[] elements = path.split(";")[0].split("\\/");
		IMendixObject leaf = rootEntity;
		for (int i = 1; i < elements.length; i++) {
			String memberName = elements[i];
			if (!memberName.contains("."))
				break;

			List<IMendixObject> childEntities = Core.retrieveByPath(context, leaf, memberName, true);
			if (childEntities.size() == 0) {
				IMetaAssociation association = Core.getMetaAssociation(memberName);
				String childType = association.getParent().getName();
				leaf = Core.instantiate(context, childType);
			} else
				leaf = childEntities.get(0);
		}
		return leaf;
	}

	private IMendixObject createLeafParent() {
		String[] elements = path.split(";")[0].split("\\/");
		IMendixObject leaf = rootEntity;
		for (int i = 1; i < elements.length - 1; i++) {
			String memberName = elements[i];
			List<IMendixObject> childEntities = Core.retrieveByPath(context, leaf, memberName, true);
			if (childEntities.size() == 0) {
				IMetaAssociation association = Core.getMetaAssociation(memberName);
				String childType = association.getParent().getName();
				if (childType.equals(rootEntity.getMetaObject().getName())) {
					childType = association.getChild().getName();
				}
				leaf = Core.instantiate(context, childType);
			} else
				leaf = childEntities.get(0);
		}
		return leaf;
	}

	private String parseAttribute() {
		String[] elements = path.split(";")[0].split("\\/");
		String memberName = elements[elements.length - 1].trim();
		if (memberName.contains("."))
			return "";
		return memberName;
	}

	private String parseAssociation() {
		String[] elements = path.split(";")[0].split("\\/");
		String memberName = elements[elements.length - 1].trim();
		return memberName;
	}

	private Map<String, Object> parseInstructions() {
		Hashtable<String, Object> instuctions = new Hashtable<>();
		String[] elements = path.split(";");
		for (int i = 1; i < elements.length; i++) {
			String instruction = elements[i].trim();
			Object argument = "";
			if (instruction.startsWith(TcDataMapper.INSTRUCTION_DATE)) {
				argument = new SimpleDateFormat(instruction.split("=")[1]);
				instruction = instruction.split("=")[0];
			}
			instuctions.put(instruction, argument);
		}
		return instuctions;
	}
}
