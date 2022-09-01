package kded.fm.dis.formplugin;

import java.util.Date;
import java.util.EventObject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import kd.bos.bill.BillShowParameter;
import kd.bos.bill.OperationStatus;
import kd.bos.dataentity.entity.DynamicObject;
import kd.bos.dataentity.entity.DynamicObjectCollection;
import kd.bos.dataentity.entity.LocaleString;
import kd.bos.entity.datamodel.ListSelectedRowCollection;
import kd.bos.form.FormShowParameter;
import kd.bos.form.ShowType;
import kd.bos.form.StyleCss;
import kd.bos.form.container.Wizard;
import kd.bos.form.control.Button;
import kd.bos.form.control.Steps;
import kd.bos.form.control.StepsOption;
import kd.bos.form.control.events.StepEvent;
import kd.bos.form.control.events.WizardStepsListener;
import kd.bos.form.field.BasedataEdit;
import kd.bos.form.field.events.BeforeF7ViewDetailEvent;
import kd.bos.form.plugin.AbstractFormPlugin;
import kd.bos.list.IListView;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.servicehelper.BusinessDataServiceHelper;
import kd.bos.servicehelper.QueryServiceHelper;
import kd.bos.servicehelper.operation.SaveServiceHelper;

public class DisRecordBillFormPlugin extends AbstractFormPlugin implements Consumer<BeforeF7ViewDetailEvent>, WizardStepsListener {
	@Override
	public void initialize() {
		// TODO Auto-generated method stub
		super.initialize();
	}
	@Override
	public void registerListener(EventObject e) {
		// TODO Auto-generated method stub
		super.registerListener(e);
		BasedataEdit basedataEdit = this.getView().getControl("kded_scheme");
		basedataEdit.addBeforeF7ViewDetailListener(this);
		addItemClickListeners(new String[] {"tbmain" });
		this.addClickListeners("kded_btnok");
		Wizard wizard = this.getControl("kded_wizardap");
		wizard.addWizardStepsListener(this);
	}
	@Override
	public void afterBindData(EventObject e) {
		// TODO Auto-generated method stub
		super.afterBindData(e);
		String billstatus= (String) this.getModel().getValue("kded_recordbillstatus");
		if("B".equals(billstatus)) {
			this.getView().setVisible(true, "kded_btnok");
		}

		Wizard wizard = this.getControl("kded_wizardap");
		// 获取设计时的步骤条设置
		List<StepsOption> stepsOptions = wizard.getStepsOptions();
		stepsOptions.clear();
		//获取消毒分录的数据进行动态构造向导控件的步骤条
		DynamicObjectCollection recordEntryCols=this.getModel().getEntryEntity("entryentity");
		int i=0;
		int currentindex=-1;
		if(this.getPageCache().get("currentstep")!=null) {
			this.getPageCache().remove("currentstep");
		}
		for(DynamicObject entrydata:recordEntryCols ) {
			StepsOption stepsOption0 = new StepsOption();
			DynamicObject dislevel = entrydata.getDynamicObject("kded_dislevel");
			stepsOption0.setTitle(new LocaleString(dislevel.getString("name")));
			DynamicObject disstep= entrydata.getDynamicObject("kded_disstep");
			stepsOption0.setDescription(new LocaleString(disstep.getString("name")));
			//已完成的步骤标记向导步骤为结束
			if("C".equals(entrydata.get("kded_disstatus"))) {
				stepsOption0.setStatus(Steps.FINISH); 
				i++;
			}	
			//未进行的步骤标记向导步骤为PROCESS
			else if("B".equals(entrydata.get("kded_disstatus"))){
				stepsOption0.setStatus(Steps.PROCESS);
				i++;
			}else if("A".equals(entrydata.get("kded_disstatus"))) {
				stepsOption0.setStatus(Steps.PROCESS);
				currentindex=i;//记录进行中的步骤
				this.getPageCache().put("currentstep",String.valueOf(currentindex));
				i++;
			}
			stepsOptions.add(stepsOption0);		
			this.getModel().setValue("kded_picturefield", this.getModel().getValue("kded_dispicture", i-1));
		}
		// 更新向导控件步骤条设置
		wizard.setWizardStepsOptions(stepsOptions);
		
		// 设置当前节点
		Map<String, Object> currentStepMap = new HashMap<>();			
		if(currentindex>=0) {	
		currentStepMap.put("currentStep", currentindex);
		currentStepMap.put("currentStatus", Steps.PROCESS);
					
		}else {
			currentStepMap.put("currentStep", i-1);
			currentStepMap.put("currentStatus", Steps.FINISH);
			this.getView().setVisible(false, "kded_btnok");
			}
			// 更新当前节点
			wizard.setWizardCurrentStep(currentStepMap);
					
		
	}

	
	@Override
	public void update(StepEvent paramStepEvent) {
		// TODO Auto-generated method stub
		//拿到当前切换的步骤是不是进行中的步骤，是则需要将完成按钮显示出来
		int stepint = paramStepEvent.getValue();//拿到当前切换的步骤
		this.getView().setVisible(false, "kded_btnok");	
		String currentstep = this.getPageCache().get("currentstep");//进行中的步骤
		if(currentstep!=null&&currentstep.equals(String.valueOf(stepint))) {
			this.getView().setVisible(true, "kded_btnok");			
		}
		//无论切换到哪个步骤都重新拿照片出来
		this.getModel().setValue("kded_picturefield", this.getModel().getValue("kded_dispicture", stepint));
	}
	
		@Override
		public void click(EventObject evt) {
			// TODO Auto-generated method stub
			super.click(evt);
			Button bt = (Button) evt.getSource();
			if("kded_btnok".equals(bt.getKey())) {
				if( this.getModel().getValue("kded_picturefield")==null||"".equals(this.getModel().getValue("kded_picturefield"))) {
					this.getView().showMessage("必须上传消毒步骤的照片！");
				}else{
					//做下一步操作时的处理逻辑
					DynamicObjectCollection recordEntryCols=this.getModel().getEntryEntity("entryentity");
					int i=1;
					for(DynamicObject entrydata:recordEntryCols ) {
					
						//进行中的步骤改成已完成
						if("A".equals(entrydata.get("kded_disstatus"))) {
							entrydata.set("kded_disstatus", "C");
							entrydata.set("kded_dispicture", this.getModel().getValue("kded_picturefield"));
							entrydata.set("kded_finishtime",new Date());
						}
						//未进行的改成进行中
						if("B".equals(entrydata.get("kded_disstatus"))&&i==1){
							entrydata.set("kded_disstatus", "A");
							i++;
						}
					}
					if(i==1) {
						//所有步骤全部完成，则消毒记录单和人员申请单都整单完成
						this.getModel().setValue("kded_recordbillstatus", "B");
						this.getView().invokeOperation("save");
	
						this.getView().updateView();
						
						//人员申请单状态改为已完成消毒
						String recordBillNo = (String) this.getModel().getValue("billno");
						QFilter Filter = new QFilter("kded_recordbillno", QCP.equals, recordBillNo);
						DynamicObject apply = BusinessDataServiceHelper.loadSingle("kded_xxapplybill", "id,*,billno,billstatus", new QFilter[] {Filter});						
						if(apply!=null) {
						apply.set("billstatus", "E");
						SaveServiceHelper.save(new DynamicObject[] {apply});
						}
					} else {
						//最后调用保存并刷新，刷新可以重新调用afterBindData事件
						this.getView().invokeOperation("save");					
						this.getView().updateView();
					}
										
				}
			}
		}
	
	@Override
	public void accept(BeforeF7ViewDetailEvent arg0) {
		// TODO Auto-generated method stub
		BasedataEdit basedataEdit  = (BasedataEdit) arg0.getSource();
		arg0.setCancel(true);
		QFilter filter = new QFilter("kded_scheme.id","=",arg0.getPkId());
		DynamicObject detail = BusinessDataServiceHelper.loadSingle("kded_schemechangedetail", "id,*",new QFilter[] {filter});
		BillShowParameter showParameter = new BillShowParameter();
	    showParameter.setFormId("kded_schemechangedetail");
	    showParameter.getOpenStyle().setShowType(ShowType.Modal);
	    showParameter.setStatus(OperationStatus.VIEW);
	    //设置弹出页面的大小，高600宽800
	    StyleCss inlineStyleCss = new StyleCss();
	    inlineStyleCss.setHeight("600");
	    inlineStyleCss.setWidth("800");
	    showParameter.getOpenStyle().setInlineStyleCss(inlineStyleCss);
	    showParameter.setPkId(detail.getPkValue());
	    showParameter.setCaption("消毒方案变更详情单");
	    //this.getView().showForm(showParameter);
	    
	    ListSelectedRowCollection rows = ((IListView)this.getView()).getSelectedRows();
	    if(!rows.isEmpty()&& rows.size()==1) {
	    FormShowParameter formShowParameter = new FormShowParameter();
	    formShowParameter.setFormId("动态表单标识");
	    showParameter.getOpenStyle().setShowType(ShowType.Modal);
	    //将当前选中行的消毒方案id传给【分配组织动态表单】
	    showParameter.setCustomParam("schemeid", rows.get(0).getPrimaryKeyValue());
	    this.getView().showForm(formShowParameter);
	    } else {
	    	this.getView().showErrorNotification("请先选中一张并且不允许选择多行");
	    }
	}

}
