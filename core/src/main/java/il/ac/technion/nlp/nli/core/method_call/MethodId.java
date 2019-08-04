package il.ac.technion.nlp.nli.core.method_call;

import com.ofergivoli.ojavalib.exceptions.UncheckedNoSuchMethodException;
import com.ofergivoli.ojavalib.reflection.ReflectionUtils;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Objects;

/**
 * This is the Serializable equivalent of {@link java.lang.reflect.Method}.
 * We prefer to have fields which are class canonical name rather than Class<?>, so that serialization would work even
 * if the referred classes no longer exist.
 *
 * @author Ofer Givoli <ogivoli@cs.technion.ac.il>
 */
public class MethodId implements Serializable {

	/**
	 * Credit for inspiration: Andy from [http://stackoverflow.com/questions/4919205/java-serializing-methods].
	 */

	private static final long serialVersionUID = 6573345602120273632L;
	
	private final String declaringClassCanonicalName;
	private final String name;
	private final ArrayList<String> parameterClassNames;
	
	
	public MethodId(Class<?> declaringClass, String name, Class<?>... parameterTypes) {
		this.declaringClassCanonicalName = declaringClass.getCanonicalName();
		this.name = name;
		parameterClassNames = new ArrayList<>();
		for (Class<?> paramType : parameterTypes) {
			parameterClassNames.add(paramType.getName());
		}
	}

	public MethodId(Method method) {
		this(method.getDeclaringClass(), method.getName(), method.getParameterTypes());
	}


	public Method getMethod() {
		try {
			Class<?> declaringClass = Class.forName(declaringClassCanonicalName);

			Class<?> parameterTypes[] = new Class[parameterClassNames.size()];
			for (int i = 0; i< parameterClassNames.size(); i++)
				parameterTypes[i] = ReflectionUtils.getClassByName(parameterClassNames.get(i));

			return declaringClass.getDeclaredMethod(name, parameterTypes);
		} catch (NoSuchMethodException e) {
			throw new UncheckedNoSuchMethodException();
		} catch (SecurityException e) {
			throw new Error(e);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	public String getName() {
		return name;
	}

	public String getDeclaringClassCanonicalName() {
		return declaringClassCanonicalName;
	}

	public ArrayList<String> getParameterClassNames() {
		return parameterClassNames;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		MethodId methodId = (MethodId) o;
		return Objects.equals(declaringClassCanonicalName, methodId.declaringClassCanonicalName) &&
				Objects.equals(name, methodId.name) &&
				Objects.equals(parameterClassNames, methodId.parameterClassNames);
	}

	@Override
	public int hashCode() {
		return Objects.hash(declaringClassCanonicalName, name, parameterClassNames);
	}

	@Override
	public String toString() {
		return getDeterministicUniqueString();
	}

	public String getDeterministicUniqueString() {
		return declaringClassCanonicalName + "." + name + "(" + parameterClassNames + ")";
	}

}
