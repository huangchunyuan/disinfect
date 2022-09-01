package kded.fm.dis.listplugin;

import java.util.Date;
import java.util.EventObject;
import java.util.List;
import java.util.Map;

import kd.bos.bill.BillShowParameter;
import kd.bos.bill.OperationStatus;
import kd.bos.context.RequestContext;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.entity.BadgeInfo;
import kd.bos.entity.datamodel.ListSelectedRow;
import kd.bos.entity.datamodel.ListSelectedRowCollection;
import kd.bos.form.ShowType;
import kd.bos.form.control.Toolbar;
import kd.bos.form.control.events.BeforeItemClickEvent;
import kd.bos.form.events.BeforeCreateListDataProviderArgs;
import kd.bos.form.events.HyperLinkClickArgs;
import kd.bos.form.events.SetFilterEvent;
import kd.bos.list.IListView;
import kd.bos.list.events.ListRowClickEvent;
import kd.bos.list.plugin.AbstractListPlugin;
import kd.bos.login.utils.DateUtils;
import kd.bos.mvc.list.ListDataProvider;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.QueryServiceHelper;
import kd.bos.servicehelper.user.UserServiceHelper;
import kd.bos.servicehelper.workflow.WorkflowServiceHelper;
import kd.bos.workflow.api.BizProcessStatus;

public class DisApplyBillListPlugin extends AbstractListPlugin {
	@Override
	public void setFilter(SetFilterEvent e) {
		// TODO Auto-generated method stub
		super.setFilter(e);
		long userId = RequestContext.get().getCurrUserId();
		//构造过滤条件：申请人等于当前登录用户
		QFilter filter = new QFilter("creator.id",QCP.equals,RequestContext.get().getCurrUserId());
		//构造过滤条件：申请进入车间等于当前登录用户负责的车间
		List<Long> deptIds = UserServiceHelper.getInchargeOrgs(userId, true);		
		QFilter deptfilter = new QFilter("kded_applyorg.id",QCP.in,deptIds);
		filter.or(deptfilter);
		e.getQFilters().add(filter);
		
	}
	@Override
	public void afterBindData(EventObject e) {
	
		// TODO Auto-generated method stub
		super.afterBindData(e);	
		//计算已审核的申请单数
		IListView listView = (IListView) this.getView();
		ListSelectedRowCollection cols = listView.getCurrentListAllRowCollection();
		int count=0;
		for(ListSelectedRow row:cols) {
			if("C".equals(row.getBillStatus().toString())) {
				count=count+1;
			}
		}
		//设置工具栏徽标数
		Toolbar toolbar = this.getView().getControl("toolbarap");
		BadgeInfo badgeInfo= new BadgeInfo();
		badgeInfo.setColor("#ff0000");
		badgeInfo.setCount(count);
		badgeInfo.setShowZero(true);
		toolbar.setBadgeInfo("kded_stardis", badgeInfo);
	}
	@Override
	public void billListHyperLinkClick(HyperLinkClickArgs args) {
		// TODO Auto-generated method stub
		super.billListHyperLinkClick(args);
		if("kded_recordbillno".equals(args.getFieldName())) {
			//取消原来的打开事件
			args.setCancel(true);
			//自己构造消毒记录单查看界面参数，并进行打开
			ListSelectedRowCollection rows = ((IListView)this.getView()).getSelectedRows();
		    DynamicObject applyBill = BusinessDataServiceHelper.loadSingle(rows.get(0).getPrimaryKeyValue(),"kded_xxapplybill");
		    String  billno = (String) applyBill.get("kded_recordbillno");
		    QFilter filter = new QFilter("billno",QCP.equals,billno);
		    DynamicObject  recordBill = BusinessDataServiceHelper.loadSingle("kded_xxrecordbill", "id", new QFilter[] {filter});
			BillShowParameter showParameter = new BillShowParameter();
		    showParameter.setFormId("kded_xxrecordbill");
		    showParameter.getOpenStyle().setShowType(ShowType.MainNewTabPage);
		    showParameter.setStatus(OperationStatus.VIEW);
		    showParameter.setPkId(recordBill.get("id"));
		    showParameter.setCaption("消毒记录单详情单");
		    this.getView().showForm(showParameter);
			
		}
	}
	@Override
	public void listRowClick(ListRowClickEvent evt) {
		// TODO Auto-generated method stub
		super.listRowClick(evt);
		 ListSelectedRowCollection rows = ((IListView)this.getView()).getSelectedRows();
		 if(rows.size()>0) {
			 if(!"C".equals(rows.get(0).getBillStatus())){
				 this.getView().setEnable(false,"kded_stardis");
			 }else {
				 this.getView().setEnable(true,"kded_stardis");
			 }
		 }else {
			 this.getView().setEnable(false,"kded_startdis");
		 }
	}
	@Override
	public void beforeItemClick(BeforeItemClickEvent evt) {
		// TODO Auto-generated method stub
		super.beforeItemClick(evt);
		if("kded_startdis".equals(evt.getItemKey())){
		ListSelectedRowCollection rows = ((IListView)this.getView()).getSelectedRows();		
		if(rows.size()==1) {
			//判断当前时间是不是申请进入日期当天
			 DynamicObject applyBill = BusinessDataServiceHelper.loadSingle(rows.get(0).getPrimaryKeyValue(),"kded_xxapplybill");
			 Date applydate=(Date) applyBill.get("kded_applytime");
	         Date totaldate=new Date();
	        if(! DateUtils.isSameDay(applydate, totaldate))
	        {
	        evt.setCancel(true);
	        this.getView().showMessage("申请进入日期当天才能开始消毒！");
	        }
	        //判断当前操作用户是不是申请车间负责人
	        DynamicObject chejian=(DynamicObject)applyBill.getDynamicObject("kded_applyorg");
			List<Long> Mangers =UserServiceHelper.getManagersOfOrg(chejian.getLong("id"));		
			if(!Mangers.contains(RequestContext.get().getCurrUserId())) {
				evt.setCancel(true);
			    this.getView().showMessage("你不是【"+chejian.getString("name")+"】的负责人，没有权限进行消毒！");
			}
		}else {
			this.getView().showErrorNotification("请先选中一行!");
		}
		}
		
	}
	@Override
	public void beforeCreateListDataProvider(BeforeCreateListDataProviderArgs args) {
		args.setListDataProvider(new ListDataProvider() {
			private final static String KEY_CURRENTAPPROVER = "kded_currentapprover";
			/**
			 * 加载列表数据
			 * @remark
			 * 获取系统自动加载的列表数据，然后对内容进行修正
			 */
			@Override
			public DynamicObjectCollection getData(int arg0, int arg1) {
				DynamicObjectCollection rows = super.getData(arg0, arg1);
				for(DynamicObject row : rows){
					String businessKey = row.getPkValue().toString();
					//单据是否在流程中
					boolean inProcess = WorkflowServiceHelper.inProcess(businessKey);
					if(inProcess) {
						//获取流程节点处理人，赋值到列表当前处理人字段
						List<Long> approverByBusinessKey = WorkflowServiceHelper.getApproverByBusinessKey(row.getPkValue().toString());
						Map<String, List<BizProcessStatus>> map = WorkflowServiceHelper.getBizProcessStatus(new String[] {row.getPkValue().toString()});
						List<BizProcessStatus> node = map.get(row.getPkValue().toString());
						node.forEach((e) -> {
							String nodeStr = e.getCurrentNodeName();
							String auditor = e.getParticipantName();
							if (auditor != null && !"".equals(auditor.trim())) {
								nodeStr = nodeStr + " / " + auditor;}
							row.set(KEY_CURRENTAPPROVER, nodeStr);
						});
												
					}
				}

				return rows;
			}
		});
	
	}
}
