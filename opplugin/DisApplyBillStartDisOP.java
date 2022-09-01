package kded.fm.dis.opplugin;

import java.util.Date;
import java.util.List;
import java.util.Map;

import kd.bos.dataentity.OperateOption;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.operate.result.OperationResult;
import kd.bos.entity.plugin.AbstractOperationServicePlugIn;
import kd.bos.entity.plugin.AddValidatorsEventArgs;
import kd.bos.entity.plugin.PreparePropertysEventArgs;
import kd.bos.entity.plugin.args.AfterOperationArgs;
import kd.bos.entity.plugin.args.EndOperationTransactionArgs;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.AttachmentServiceHelper;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.operation.SaveServiceHelper;

public class DisApplyBillStartDisOP extends AbstractOperationServicePlugIn{
	// TODO Auto-generated constructor stub
	@Override
	public void onPreparePropertys(PreparePropertysEventArgs e) {
		// TODO Auto-generated method stub
		super.onPreparePropertys(e);
		e.getFieldKeys().add("kded_applyorg");
		e.getFieldKeys().add("creator");
		e.getFieldKeys().add("kded_dept");
		e.getFieldKeys().add("kded_company");
		e.getFieldKeys().add("kded_applydate");
		e.getFieldKeys().add("kded_recordbillno");
	}
	@Override
	public void afterExecuteOperationTransaction(AfterOperationArgs e) {
		// TODO Auto-generated method stub
		super.afterExecuteOperationTransaction(e);
	}
	@Override
	public void onAddValidators(AddValidatorsEventArgs e) {
		// TODO Auto-generated method stub
		super.onAddValidators(e);
		
	}
	
	@Override
	public void endOperationTransaction(EndOperationTransactionArgs e) {
		// TODO Auto-generated method stub
		super.endOperationTransaction(e);
		DynamicObject[] dataEntities = e.getDataEntities();
		DynamicObject formData = dataEntities[0];
		//获取申请进入车间匹配对应方案
		DynamicObject org=(DynamicObject) formData.get("kded_applyorg");
		DynamicObject applier=(DynamicObject) formData.get("creator");
		DynamicObject dept=(DynamicObject) formData.get("kded_dept");
		DynamicObject company=(DynamicObject) formData.get("kded_company");
		Date applydate= (Date) formData.get("kded_applydate");
		QFilter schameFilter = new QFilter("kded_entryentityorg.kded_useorg.number", QCP.equals, org.getString("number"));
		QFilter schameFilter1 = new QFilter("kded_versionstatus","=","A"); 
		schameFilter.and(schameFilter1);
		DynamicObject scheme = BusinessDataServiceHelper.loadSingle("kded_disscheme", "id,*,kded_entryentitylevel.id,kded_entryentitylevel.kded_dislevel,kded_subentryentitystep.id,kded_subentryentitystep.kded_disstep", new QFilter[] {schameFilter});
		if(scheme!=null) {
		DynamicObject newDynamicObject = BusinessDataServiceHelper.newDynamicObject("kded_xxrecordbill");
		//先塞单据头的字段值
		newDynamicObject.set("kded_applyorg", org);
		newDynamicObject.set("creator",applier);
		newDynamicObject.set("kded_dept",dept);
		newDynamicObject.set("kded_company",company);
		newDynamicObject.set("kded_recordbillstatus", "A");
		newDynamicObject.set("kded_applydate", applydate);
		newDynamicObject.set("kded_scheme", scheme.get("id"));
		//再构造消毒分录的数据
		//遍历消毒方案的消毒等级单据体数据
		DynamicObjectCollection	schemeEntryCollection=scheme.getDynamicObjectCollection("kded_entryentitylevel");
		int i=0;
		for(DynamicObject entrydata:schemeEntryCollection) {
			//获取消毒等级单据体下的消毒步骤子单据体集合
			DynamicObjectCollection	schemeSubEntryCollection=	entrydata.getDynamicObjectCollection("kded_subentryentitystep");
			int size=schemeSubEntryCollection.size();
			DynamicObject dislevel=(DynamicObject) entrydata.get("kded_dislevel");
			
			for(DynamicObject subEntryData:schemeSubEntryCollection) {
				DynamicObject disstep=(DynamicObject) subEntryData.get("kded_disstep");
				DynamicObjectCollection recordEntryCols = newDynamicObject.getDynamicObjectCollection("entryentity");
				DynamicObject recordEntryCol =  new DynamicObject(recordEntryCols.getDynamicObjectType());
				recordEntryCol.set("kded_dislevel", dislevel);
				recordEntryCol.set("kded_disstep", disstep);
				if(i==0)
					recordEntryCol.set("kded_disstatus", "A");
				else 
					recordEntryCol.set("kded_disstatus", "B");
				recordEntryCols.add(recordEntryCol);
				i++;
			}
			
		}
		OperationResult saveOperate = SaveServiceHelper.saveOperate("kded_xxrecordbill",new DynamicObject[] {newDynamicObject},OperateOption.create());
		if(saveOperate.isSuccess()) {
		List<Object> successPkIds = saveOperate.getSuccessPkIds();
		//处理附件
		List<Map<String, Object>>attachments = AttachmentServiceHelper.getAttachments("kded_xxapplybill", formData.getPkValue(), "attachmentpanel");
		for(Map<String, Object> att : attachments) {
			att.put("lastModified", new Date().getTime());
		}
		AttachmentServiceHelper.upload("kded_xxrecordbill", successPkIds.get(0), "attachmentpanel", attachments);

		this.getOperationResult().setMessage("成功生成消毒记录单");
		//返回消毒记录单的单据编号到车辆入场申请单
		formData.set("kded_recordbillno", saveOperate.getBillNos().get("0"));
		//人员申请单状态改为消毒中
		formData.set("billstatus", "D");
		SaveServiceHelper.save(new DynamicObject[] {formData});
		}


		
	}else {
		this.getOperationResult().setSuccess(false);
		this.getOperationResult().setMessage("无法匹配到消毒方案，请先维护该组织的消毒方案");
		
	}
	}

}
