package kded.fm.dis.formplugin;

import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;

import kd.bos.algo.DataSet;
import kd.bos.context.RequestContext;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.form.control.Button;
import kd.bos.form.control.EntryGrid;
import kd.bos.form.control.Toolbar;
import kd.bos.form.control.events.BeforeClickEvent;
import kd.bos.form.control.events.BeforeItemClickEvent;
import kd.bos.form.field.BasedataEdit;
import kd.bos.form.field.events.BeforeF7SelectEvent;
import kd.bos.form.field.events.BeforeF7SelectListener;
import kd.bos.form.plugin.AbstractFormPlugin;
import kd.bos.list.ListShowParameter;
import kd.bos.orm.ORM;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.QueryServiceHelper;
import kd.bos.servicehelper.user.UserServiceHelper;

public class DisSchameFormPlugin extends AbstractFormPlugin implements BeforeF7SelectListener {
	@Override
	public void registerListener(EventObject e) {
		// TODO Auto-generated method stub
		super.registerListener(e);
		//给消毒等级基础资料字段注册BeforeF7SelectListener
		BasedataEdit step = this.getControl("kded_disstep");
		step.addBeforeF7SelectListener(this);
		Toolbar tb = this.getControl("tbmain");
		tb.addItemClickListener(this);
	}
	@Override
	public void afterCreateNewData(EventObject e) {
		// TODO Auto-generated method stub
		super.afterCreateNewData(e);
		//新增时默认带出消毒等级
		DynamicObjectCollection cols = QueryServiceHelper.query("kded_dislevel", "id,number,name", null, "number asc");
		this.getModel().batchCreateNewEntryRow("kded_entryentitylevel", cols.size());
		for(int i = 0;i<cols.size();i++) {
			this.getModel().setValue("kded_dislevel", cols.get(i).get("id"),i);
		}
		
	}
	@Override
	public void afterBindData(EventObject e) {
		// TODO Auto-generated method stub
		super.afterBindData(e);
		//获取单据体控件编程模型
		EntryGrid entryGrid = this.getControl("kded_entryentitylevel");
		//设置单据体指定行的背景色
		entryGrid.setRowBackcolor("yellow", new int[]{0});
		entryGrid.setRowBackcolor("green", new int[]{1});
		entryGrid.setRowBackcolor("red", new int[]{2});
	}
	@Override
	public void beforeF7Select(BeforeF7SelectEvent arg0) {
		// TODO Auto-generated method stub
		if("kded_disstep".equals(arg0.getProperty().getName())) {
			//获取选中等级
			int index = this.getModel().getEntryCurrentRowIndex("kded_entryentitylevel");
			DynamicObject level = (DynamicObject) this.getModel().getValue("kded_dislevel", index);
			if(level==null) {
				this.getView().showMessage("请先维护等级");
				arg0.setCancel(true);
			}else {
				//设置F7列表过滤条件
				QFilter filter = new QFilter("group.number",QCP.equals,level.get("number"));
				ListShowParameter listShowParameter = (ListShowParameter) arg0.getFormShowParameter();
				listShowParameter.getListFilterParameter().setFilter(filter);
				//设置F7左树过滤条件
				QFilter treeFilter = new QFilter("number",QCP.equals,level.get("number"));
				listShowParameter.getTreeFilterParameter().getQFilters().add(treeFilter);
			}
		}
		
	}
	@Override
	public void beforeClick(BeforeClickEvent evt) {
		// TODO Auto-generated method stub
		super.beforeClick(evt);
		
	}
	@Override
	public void beforeItemClick(BeforeItemClickEvent evt) {
		// TODO Auto-generated method stub
		super.beforeItemClick(evt);
		if("bar_save".equals(evt.getItemKey())) {
		long userId = RequestContext.get().getCurrUserId();
		QFilter f1 = new QFilter("id", "=", Long.valueOf(userId));
		QFilter f2 = new QFilter("entryentity.isincharge", "=", "1");
		QFilter f3 = new QFilter("entryentity.dpt.name", QCP.like, "%车间%");
		ORM orgORM = ORM.create();
		DataSet ds = orgORM.queryDataSet(UserServiceHelper.class.getName(), "bos_user", "entryentity.dpt.id", new QFilter[]{f1, f2,f3});
		if(ds.isEmpty()) {
			//this.getView().showErrorNotification("你不是车间负责人，无法发布消毒方案");
			//evt.setCancel(true);
		}
	}
	}
	

}
