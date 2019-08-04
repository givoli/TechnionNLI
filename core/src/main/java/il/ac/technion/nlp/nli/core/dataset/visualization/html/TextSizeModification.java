package il.ac.technion.nlp.nli.core.dataset.visualization.html;

/**
 * Represent text size modification relative to the containing element.
 */
public enum TextSizeModification {
	SMALLER_X, SMALLER, BIGGER, BIGGER_X, BIGGER_XX, BIGGER_XXX;

	/**
	 * Not that 'NORMAL' is not exactly 1.0em.
     */
	public double getCssEm() {
		switch (this) {
			case SMALLER_X:
				return 0.8;
			case SMALLER:
				return 0.9;
			case BIGGER:
				return 1.13;
			case BIGGER_X:
				return 1.2;
			case BIGGER_XX:
				return 1.3;
			case BIGGER_XXX:
				return 1.5;

			default:
				throw new Error();
		}
	}
}
