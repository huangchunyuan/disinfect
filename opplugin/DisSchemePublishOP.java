package kded.fm.dis.opplugin;

import java.util.Date;
import java.util.List;

import kd.bos.dataentity.OperateOption;
import kd.bos.dataentity.entity.CloneUtils;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.operate.result.OperationResult;
import kd.bos.entity.plugin.AbstractOperationServicePlugIn;
import kd.bos.entity.plugin.AddValidatorsEventArgs;
import kd.bos.entity.plugin.args.AfterOperationArgs;
import kd.bos.entity.plugin.args.EndOperationTransactionArgs;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.AttachmentServiceHelper;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.QueryServiceHelper;
import kd.bos.servicehelper.operation.SaveServiceHelper;

public class DisSchemePublishOP extends AbstractOperationServicePlugIn {
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
		//拿到当前操作对象消毒方案及id
		DynamicObject[] dataEntities = e.getDataEntities();
		DynamicObject formData = dataEntities[0];
		//判断方案是否被消毒记录单引用
		long id = (long) formData.get("id");
		if(!(id==0L)) {
		QFilter filter = new QFilter("kded_scheme.id", QCP. equals, formData.get("id")); 
		boolean isExist = QueryServiceHelper.exists("kded_xxrecordbill", new QFilter[] {filter});
		if(isExist) {
		//克隆当前页面单据数据对象
		DynamicObject newObj = (DynamicObject) new CloneUtils(false,true).clone(formData);
		newObj.set("kded_version", newObj.getInt("kded_version")+1);//当前对象版本号+1
		//第一个false代表会复制字段值，第二个参数true代表清除主键值
		
		//查询原来的方案,版本状态改成旧版本
		DynamicObject oldObj = BusinessDataServiceHelper.loadSingle(formData.getPkValue(), "kded_disscheme");
		oldObj.set("kded_versionstatus", "B");
		SaveServiceHelper.save(new DynamicObject[] {newObj, oldObj });//保存新旧版本的对象
		return;
		} 
	}
		//直接保存
		SaveServiceHelper.save(new DynamicObject[] {formData});
	}
	}


