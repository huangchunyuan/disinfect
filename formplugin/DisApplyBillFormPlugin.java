package kded.fm.dis.formplugin;

import java.util.EventObject;
import java.util.Map;

import org.apache.commons.collections4.map.HashedMap;

import kd.bos.context.RequestContext;
import kd.bos.form.ClientProperties;
import kd.bos.form.plugin.AbstractFormPlugin;
import kd.bos.servicehelper.org.OrgUnitServiceHelper;
import kd.bos.servicehelper.user.UserServiceHelper;

public class DisApplyBillFormPlugin extends AbstractFormPlugin {
	@Override
	public void afterCreateNewData(EventObject e) {
		// TODO Auto-generated method stub
		super.afterCreateNewData(e);
		//默认带出申请人的主职部门及公司11
		long userId = RequestContext.get().getCurrUserId();
		long deptId = UserServiceHelper.getUserMainOrgId(userId);
		this.getModel().setValue("kded_dept",deptId);
		Map<String, Object> companyByOrg = OrgUnitServiceHelper.getCompanyfromOrg(deptId);
		this.getModel().setValue("kded_company",companyByOrg.get("id"));

	}
	@Override
	public void afterBindData(EventObject e) {
		// TODO Auto-generated method stub
		super.afterBindData(e);
		//遍历单据状态
		String status = (String) this.getModel().getValue("billstatus");
		String color = "#666666";
		switch(status) {
		case "C":
			color = "#5fbf00";
			break;
		case "B":
			color = "#ffaa56";
			break;
		case "E":
			color = " #FF0000";
			break;
		default :
			break;
		}
		//封装前景色属性
		Map<String,Object> map = new HashedMap<>();
		map.put(ClientProperties.ForeColor, color);
		//更新控件元数据属性
		this.getView().updateControlMetadata("billstatus", map);
	}
	
}
