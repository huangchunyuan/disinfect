package kded.fm.dis.formplugin;

import java.util.EventObject;

import org.antlr.v4.parse.ANTLRParser.throwsSpec_return;

import com.grapecity.documents.excel.B;

import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.dataentity.metadata.dynamicobject.DynamicObjectType;
import kd.bos.entity.datamodel.events.PropertyChangedArgs;
import kd.bos.form.control.Button;
import kd.bos.form.plugin.AbstractFormPlugin;
import kd.bos.orm.ORM;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.QueryServiceHelper;
import kd.bos.servicehelper.operation.SaveServiceHelper;

public class DisAssignOrgFormPlugin extends AbstractFormPlugin {
	@Override
	public void registerListener(EventObject e) {
		// TODO Auto-generated method stub
		super.registerListener(e);
		this.addClickListeners("btnok");
	}
	@Override
	public void afterCreateNewData(EventObject e) {
		// TODO Auto-generated method stub
		super.afterCreateNewData(e);
		this.getModel().deleteEntryData("kded_entryentity");
		long schemeId = this.getView().getFormShowParameter().getCustomParam("schemeid");
		QFilter filter = new QFilter("id","=",schemeId);
		DynamicObjectCollection cols = QueryServiceHelper.query("kded_disscheme", "id,kded_entryentityorg.kded_useorg.id", new QFilter[] {filter} );
		this.getModel().batchCreateNewEntryRow("kded_entryentity", cols.size());
		for(int i = 0;i<cols.size();i++) {
			this.getModel().setValue("kded_useorg", cols.get(i).get("kded_entryentityorg.kded_useorg.id"),i);
		}
	}
	@Override
	public void propertyChanged(PropertyChangedArgs e) {
		// TODO Auto-generated method stub
		super.propertyChanged(e);
		if("kded_useorg".equals(e.getProperty().getName())) {
			if(!(e.getChangeSet() [0].getNewValue()== null)) {
			//获取刚选择的组织
			 DynamicObject neworg = (DynamicObject) e.getChangeSet() [0].getNewValue();
			 

			//构造过滤条件
			//方案组织分录等于当前所选组织
			QFilter schameFilter = new QFilter("kded_entryentityorg.kded_useorg.id", QCP. equals, neworg.get("id")); 
			QFilter schameFilter1 = new QFilter("kded_versionstatus", "=", "A");//最新版本
			schameFilter.and(schameFilter1);
			 
			//查询该组织是否被分配了最新方案：
			boolean isExist = QueryServiceHelper.exists("kded_disscheme", new QFilter[] {schameFilter});

			//是的话清除当前行的组织字段
			if(isExist) {
			this.getModel().setValue("kded_useorg",null, e.getChangeSet() [0]. getRowIndex());
			this.getView().showErrorNotification("当前所选组织"+neworg.getString("name")+"已经被其他方案分配，请重新选择其他组织");
			}
			}

		}
		
	}
	@Override
	public void click(EventObject evt) {
		// TODO Auto-generated method stub
		super.click(evt);
		Button bt = (Button) evt.getSource();
		if("btnok".equals(bt.getKey())) {
			//清空消毒方案的原来组织的分录
			long schemeId = this.getView().getFormShowParameter().getCustomParam("schemeid");
			QFilter filter = new QFilter("id","=",schemeId);
			QFilter filter1 = new QFilter("billno","=","RECORD-20220809-0001");
			
			DynamicObject object = BusinessDataServiceHelper.loadSingle(schemeId, "kded_disscheme", "id,kded_entryentityorg,kded_entryentityorg.id,kded_entryentityorg.kded_useorg");
			DynamicObjectCollection cols = object.getDynamicObjectCollection("kded_entryentityorg");
			//cols.get(0).get("kded_userorg");
			cols.clear();
			//获取当前表单的组织分录
			DynamicObjectCollection nowCols = (DynamicObjectCollection) this.getModel().getDataEntity(true).getDynamicObjectCollection("kded_entryentity");
			for (DynamicObject col : nowCols) {
			//新增一行组织分录
			DynamicObject newObj = ORM.create().newDynamicObject(cols.getDynamicObjectType());
			newObj.set("kded_useorg", col.getDynamicObject("kded_useorg").get("id")); 
			cols.add(newObj);//塞给方案对象
			}
			
			SaveServiceHelper.save(new DynamicObject[] {object});//保存方案
			this.getView().getParentView().updateView();
			this.getView().sendFormAction(this.getView().getParentView());
			this.getView().close();

		}
		
	}
		

}
