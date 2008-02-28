package org.eclipse.equinox.internal.security.ui;

/**
 * Used by the X509CertificateAttributeContentProvider.
 * 
 * Objects of this class contain a decomposition of an attribute in an X509 certificate so it can be
 * displayed in the UI.
 *
 */
public class X509CertificateAttribute {
	// Description of the field
	//   NOTE: This will show in the UI so it should be loaded from a translatable properties file. 
	private String fieldDescription;

	// A String representation of the value of the field.  May have undergone some
	//   transformation to make it "pretty" in the UI
	private String stringVal;

	// The raw data object from inside the certificate.  Most likely whatever object the getter method
	//    for the field returns.
	private Object rawValue;

	public X509CertificateAttribute(String propDescription, String StringVal, Object objValue) {
		super();
		fieldDescription = propDescription;
		stringVal = StringVal;
		rawValue = objValue;
	}

	public X509CertificateAttribute(String propDescription, String StringVal) {
		super();
		fieldDescription = propDescription;
		stringVal = StringVal;
		rawValue = null;
	}

	public String getDescription() {
		return fieldDescription;
	}

	public String getStringValue() {
		return stringVal;
	}

	public Object getValue() {
		if (rawValue == null) {
			return stringVal;
		}
		return rawValue;
	}

}
