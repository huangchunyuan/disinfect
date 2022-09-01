package kded.fm.dis.listplugin;

import java.util.EventObject;

import kd.bos.entity.datamodel.ListSelectedRowCollection;
import kd.bos.form.FormShowParameter;
import kd.bos.form.ShowType;
import kd.bos.form.control.events.ItemClickEvent;
import kd.bos.list.IListView;
import kd.bos.list.plugin.AbstractListPlugin;
import kd.occ.ocepfp.core.form.control.controls.ToolBar;

public class DisSchameListPlugin extends AbstractListPlugin {
	@Override
	public void itemClick(ItemClickEvent evt) {
		// TODO Auto-generated method stub
		super.itemClick(evt);
		if("kded_assignorg".equals(evt.getItemKey())) {
			ListSelectedRowCollection rows = ((IListView) this.getView()).getSelectedRows();
			if(!rows.isEmpty()&& rows.size()==1) {
			    FormShowParameter formShowParameter = new FormShowParameter();
			    formShowParameter.setFormId("kded_assignorgform");
			    formShowParameter.getOpenStyle().setShowType(ShowType.Modal);
			    //将当前选中行的消毒方案id传给【分配组织动态表单】
			    formShowParameter.setCustomParam("schemeid", rows.get(0).getPrimaryKeyValue());
			    this.getView().showForm(formShowParameter);
			}else {
				this.getView().showErrorNotification("请选择一行消毒方案");
			}

		}
	}
	@Override
	public void click(EventObject evt) {
		// TODO Auto-generated method stub
		super.click(evt);
		ToolBar tb = (ToolBar) evt.getSource();
	
	}

}
