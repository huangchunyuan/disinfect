package kded.fm.dis.reportplugin;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import kd.bos.algo.DataSet;
import kd.bos.algo.DataType;
import kd.bos.algo.Row;
import kd.bos.entity.report.AbstractReportListDataPlugin;
import kd.bos.entity.report.FastFilter;
import kd.bos.entity.report.FilterItemInfo;
import kd.bos.entity.report.ReportQueryParam;
import kd.bos.orm.query.QCP;
import kd.bos.orm.query.QFilter;
import kd.bos.orm.util.StringUtils;
import kd.bos.servicehelper.QueryServiceHelper;

public class DisReportListDataPlugin extends AbstractReportListDataPlugin {

	// 源数据字段
	// 采购申请单单据标识
	private static String ICLZ_PURREQ = "kded_xxrecordbill";
	// 采购组织
	private static String ICLZ_APPLYORG = "kded_applyorg";
	//物料字段标识kded_materielfield，查询时，要加上单据体标识，如下
	private static String ICLZ_MATER = "kded_entryentity.kded_materielfield";
	//申请时间
	private static String ICLZ_APPLYDATE = "kded_applydate";
	//申请数量
	private static String ICLZ_APPLYQTY = "kded_entryentity.kded_qtyfield";
	// 报表字段，报表字段要保证字段类型和字段标识和查询的源数据的一致
	private static String[] FIELDS = { "kded_applyorg", "kded_applydate", "kded_qty7", "kded_qty8", "kded_qtyall" };
	private static DataType[] DATATYPES = { DataType.LongType, DataType.LongType, DataType.LongType, DataType.LongType,
			DataType.LongType };

	@Override
	public DataSet query(ReportQueryParam arg0, Object arg1) throws Throwable {
		// TODO Auto-generated method stub
		//常用过滤条件
				List<FilterItemInfo> filters = arg0.getFilter().getFilterItems();
				FastFilter fastFilter = arg0.getFilter().getFastFilter();
				String applyorg = null;
				String materia = null;
				for (FilterItemInfo filter : filters) {
					switch (filter.getPropName()) {
					case "kded_applyorg.id":
						applyorg = (filter == null) ? null
								: String.valueOf((filter.getValue()));
					default:
						break;
					}
				}
				
				// 根据查询字段和查询条件查询采购申请单数据
				// （1）查询字段
				StringBuilder selectSettlementFields = new StringBuilder();
				selectSettlementFields.append(ICLZ_APPLYORG).append(" AS ").append(FIELDS[0]).append(", ").append(ICLZ_APPLYDATE)
						.append(" AS ").append(FIELDS[1]).append(", billno");
				// （2）查询条件ICLZ_APPLYQTY
				List<QFilter> searchFilterList = new ArrayList<>();
				//过滤掉空数据数据，add起来的过滤条件类似于sql中的 and where a="xx"
				searchFilterList.add(new QFilter(ICLZ_APPLYORG, QCP.is_notnull,""));
				searchFilterList.add(new QFilter(ICLZ_APPLYDATE, QCP.is_notnull,""));
				Date nowTime=new Date();
				Calendar calendar = Calendar.getInstance();
				int i = calendar.get(Calendar.YEAR);//当前年份
				String lastYearEnd=String.valueOf(i-1);
				lastYearEnd=lastYearEnd+"-12-31";
				//报表只显示今年的数据
				searchFilterList.add(new QFilter(ICLZ_APPLYDATE, QCP.large_than,lastYearEnd));
				if (!StringUtils.isEmpty(applyorg)) {
					searchFilterList.add(new QFilter(ICLZ_APPLYORG, QCP.equals, applyorg));
				}
				
				// （3）查询采购申请单数据.where("kded_appdate > 2020-12-31")
				DataSet dataSet = QueryServiceHelper.queryDataSet(this.getClass().getName(), ICLZ_PURREQ,
						selectSettlementFields.toString(), searchFilterList.toArray(new QFilter[] {}), null);
				for (Row row : dataSet.copy()) {
					 DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
					 Date date = df.parse(row.getTimestamp(ICLZ_APPLYDATE).toString());
					Date now = new Date();
					Long day=(now.getTime()-date.getTime())/1000/3600/24;
				}
				return null;
	}

}
