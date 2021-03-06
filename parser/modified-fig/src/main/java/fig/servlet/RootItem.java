//NOTICE: this file was modified by Ofer Givoli (i.e. it's not identical to the matching file in the original fig package).
package fig.servlet;

import fig.basic.IOUtils;

import java.io.File;

/**
 * The root item.
 */
public class RootItem extends Item {
  public final BasketView basketView;
  public final DomainView domainView;

  public RootItem(String varDir) {
    super(null, "ROOT", null);
    IOUtils.createNewDirIfNotExistsEasy(varDir);
    ServletLogInfo.logs("RootItem: varDir = " + varDir);
    addItem(this.basketView = new BasketView(this, "baskets", new File(varDir, "baskets").toString(), true));
    addItem(this.domainView = new DomainView(this, "il/ac/technion/nlp/nli/core/dataset/domains", new File(varDir, "il/ac/technion/nlp/nli/core/dataset/domains").toString())); //Added. --Ofer Givoli
  }

  public FieldListMap getMetadataFields() {
    FieldListMap fields = new FieldListMap();
    fields.add("logUpdates", "Whether to log updates").mutable = true;
    fields.add("logWorkers", "Whether to log requests sent by workers").mutable = true;
    fields.add("verbose",    "Generally print out a lot of stuff").mutable = true;
    return fields;
  }

  protected Value getIntrinsicFieldValue(String fieldName) throws MyException { // OVERRIDE
    if(fieldName.equals("logUpdates")) return new Value(""+ServletLogInfo.logUpdates);
    if(fieldName.equals("verbose"))    return new Value(""+ServletLogInfo.verbose);
    return super.getIntrinsicFieldValue(fieldName);
  }
  protected void changeIntrinsicFieldValue(String fieldName, String value) throws MyException {
         if(fieldName.equals("logUpdates")) ServletLogInfo.logUpdates = Boolean.parseBoolean(value);
    else if(fieldName.equals("verbose"))    ServletLogInfo.verbose = Boolean.parseBoolean(value);
    else super.changeIntrinsicFieldValue(fieldName, value);
  }

  public void update(UpdateSpec spec, UpdateQueue.Priority priority) throws MyException { // OVERRIDE
    super.update(spec, priority);
    updateChildren(spec, priority);
  }

  protected boolean isView() { return true; }
  protected Item newItem(String name) throws MyException { throw MyExceptions.unsupported; }
}
