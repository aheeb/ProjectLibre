/*******************************************************************************
 * The contents of this file are subject to the Common Public Attribution License 
 * Version 1.0 (the "License"); you may not use this file except in compliance with 
 * the License. You may obtain a copy of the License at 
 * http://www.projectlibre.com/license . The License is based on the Mozilla Public 
 * License Version 1.1 but Sections 14 and 15 have been added to cover use of 
 * software over a computer network and provide for limited attribution for the 
 * Original Developer. In addition, Exhibit A has been modified to be consistent 
 * with Exhibit B. 
 *
 * Software distributed under the License is distributed on an "AS IS" basis, 
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for the 
 * specific language governing rights and limitations under the License. The 
 * Original Code is ProjectLibre. The Original Developer is the Initial Developer 
 * and is ProjectLibre Inc. All portions of the code written by ProjectLibre are 
 * Copyright (c) 2012-2019. All Rights Reserved. All portions of the code written by 
 * ProjectLibre are Copyright (c) 2012-2019. All Rights Reserved. Contributor 
 * ProjectLibre, Inc.
 *
 * Alternatively, the contents of this file may be used under the terms of the 
 * ProjectLibre End-User License Agreement (the ProjectLibre License) in which case 
 * the provisions of the ProjectLibre License are applicable instead of those above. 
 * If you wish to allow use of your version of this file only under the terms of the 
 * ProjectLibre License and not to allow others to use your version of this file 
 * under the CPAL, indicate your decision by deleting the provisions above and 
 * replace them with the notice and other provisions required by the ProjectLibre 
 * License. If you do not delete the provisions above, a recipient may use your 
 * version of this file under either the CPAL or the ProjectLibre Licenses. 
 *
 *
 * [NOTE: The text of this Exhibit A may differ slightly from the text of the notices 
 * in the Source Code files of the Original Code. You should use the text of this 
 * Exhibit A rather than the text found in the Original Code Source Code for Your 
 * Modifications.] 
 *
 * EXHIBIT B. Attribution Information for ProjectLibre required
 *
 * Attribution Copyright Notice: Copyright (c) 2012-2019, ProjectLibre, Inc.
 * Attribution Phrase (not exceeding 10 words): 
 * ProjectLibre, open source project management software.
 * Attribution URL: http://www.projectlibre.com
 * Graphic Image as provided in the Covered Code as file: projectlibre-logo.png with 
 * alternatives listed on http://www.projectlibre.com/logo 
 *
 * Display of Attribution Information is required in Larger Works which are defined 
 * in the CPAL as a work which combines Covered Code or portions thereof with code 
 * not governed by the terms of the CPAL. However, in addition to the other notice 
 * obligations, all copies of the Covered Code in Executable and Source Code form 
 * distributed must, as a form of attribution of the original author, include on 
 * each user interface screen the "ProjectLibre" logo visible to all users. 
 * The ProjectLibre logo should be located horizontally aligned with the menu bar 
 * and left justified on the top left of the screen adjacent to the File menu. The 
 * logo must be at least 144 x 31 pixels. When users click on the "ProjectLibre" 
 * logo it must direct them back to http://www.projectlibre.com. 
 *******************************************************************************/
package com.projectlibre1.pm.graphic.gantt;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Shape;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;

import javax.swing.JMenu;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import com.projectlibre1.pm.graphic.graph.GraphInteractor;
import com.projectlibre1.pm.graphic.graph.GraphUI;
import com.projectlibre1.pm.graphic.model.cache.GraphicDependency;
import com.projectlibre1.pm.graphic.model.cache.GraphicNode;
import com.projectlibre1.pm.graphic.timescale.CoordinatesConverter;
import com.projectlibre1.association.InvalidAssociationException;
import com.projectlibre1.functor.IntervalConsumer;
import com.projectlibre1.pm.dependency.DependencyService;
import com.projectlibre1.pm.dependency.DependencyType;
import com.projectlibre1.pm.dependency.HasDependencies;
import com.projectlibre1.pm.scheduling.Schedule;
import com.projectlibre1.pm.scheduling.ScheduleInterval;
import com.projectlibre1.pm.scheduling.ScheduleService;
import com.projectlibre1.configuration.Configuration;
import com.projectlibre1.datatype.DurationFormat;
import com.projectlibre1.grouping.core.model.NodeModel;
import com.projectlibre1.pm.calendar.WorkCalendar;
import com.projectlibre1.pm.graphic.frames.GraphicManager;
import com.projectlibre1.pm.graphic.spreadsheet.SpreadSheet;
import com.projectlibre1.pm.graphic.spreadsheet.SpreadSheetModel;
import com.projectlibre1.pm.graphic.spreadsheet.renderer.DateRenderer;
import com.projectlibre1.pm.graphic.views.GanttView;
import com.projectlibre1.pm.scheduling.ConstraintType;
import com.projectlibre1.pm.task.Task;
import com.projectlibre1.strings.Messages;
import com.projectlibre1.util.Alert;
import com.projectlibre1.util.ClassUtils;
import com.projectlibre1.util.DateTime;
import com.projectlibre1.workflow.DragTip;
import com.projectlibre1.workflow.TaskMenu;
import com.projectlibre1.workflow.Toast;

/**
 *
 */
public class GanttInteractor extends GraphInteractor{
	private static final long serialVersionUID = -555882007216388246L;
	protected static final int BAR_MOVE_START=4;
	protected static final int BAR_MOVE_END=5;
	protected static final int PROGRESS_BAR_MOVE=6;
	protected static final int SPLIT=7;

	protected ScheduleInterval selectedInterval;
	protected int selectedIntervalNumber;
	protected double t;
	/**
	 *
	 */
	public GanttInteractor(GraphUI ui) {
		super(ui);
		popup=new GanttPopupMenu(this);
	}


    private class NodeSelectionIntervalConsumer implements IntervalConsumer{
    	private boolean consumed=false;
    	private GraphicNode node;
    	private double deltaResize0;
    	private double deltaResize1;
    	private double deltaOutside;
		private double completedDeltaT0;
		private double completedDeltaT1;
    	private ScheduleInterval completedInterval;
		private double t;
		private CoordinatesConverter coord;


    	public NodeSelectionIntervalConsumer init(double x,GraphicNode node){
    		this.t=getCoord().toTime(x);
    		consumed=false;
    		selectedInterval=null;
    		completedInterval=null;
    		selectedIntervalNumber=0;
//    		GraphicNode node=(GraphicNode)selected;
//    		long completedT=node.getCompleted();
    		coord=getCoord();
    		completedDeltaT0=coord.toDuration(config.getSelectionProgress0());
    		completedDeltaT1=coord.toDuration(config.getSelectionProgress1());
    		deltaResize0=coord.toDuration(config.getSelectionResize0());
    		deltaResize1=coord.toDuration(config.getSelectionResize1());
    		deltaOutside=coord.toDuration(config.getSelectionSquare());
    		this.node=node;
    		return this;
    	}
    	public void consumeInterval(ScheduleInterval interval){
    		if (consumed) return; //Consumer need to consume all the intervals
    		if (ClassUtils.isObjectReadOnly(((GraphicNode)selected).getNode().getImpl())) // pre
    			return;
			if (coord!=null){
				long completedT=((GraphicNode)selected).getCompleted();
	    		if (completedT>=interval.getStart()&&completedT<=interval.getEnd()){
	    			completedInterval=new ScheduleInterval(interval.getStart(),completedT);
	    			if (interval.getEnd()>interval.getStart()) completedInterval=coord.adaptSmallBarTimeInterval(completedInterval,node,config);
	    		}
				interval=coord.adaptSmallBarTimeInterval(interval, node,config);

			}
    		if (t<interval.getStart()&&t>=interval.getStart()-deltaOutside){
    			if (selectedIsNonSummaryNode()) state=BAR_MOVE_START;
    		}else if (t>interval.getEnd()&&t<=interval.getEnd()+deltaOutside){
    			if (selectedIsNonSummaryNode()) state=BAR_MOVE_END;
    		}else if (t<interval.getStart()||t>interval.getEnd()){
    			selectedIntervalNumber++;
    			return;
       		}else if (completedInterval!=null&&t>=completedInterval.getEnd()-completedDeltaT0&&t<=completedInterval.getEnd()+completedDeltaT1&&selectedZone!=null&&selectedZone.getZoneId()==GanttUI.PROGRESS_BAR_ZONE_ID){
       			if (selectedIsNonSummaryNode()) state=PROGRESS_BAR_MOVE;
    		}else if (t<=interval.getStart()+deltaResize0){
    			if (selectedIsNonSummaryNode()) state=BAR_MOVE_START;
    		}else if (t>=interval.getEnd()-deltaResize1){
    			if (selectedIsNonSummaryNode()) state=BAR_MOVE_END;
    		} else state= BAR_MOVE;
    		selectedInterval=interval;
			consumed=true;
     	}
    }
    private NodeSelectionIntervalConsumer nodeSelectionIntervalConsumer=new NodeSelectionIntervalConsumer();

    protected void computeNodeSelection(double x,double y){
		//would have prefered an iterator
    	GraphicNode node=(GraphicNode)selected;
		node.consumeIntervals(nodeSelectionIntervalConsumer.init(x,node));
    }


    protected Shape getBarShadowBounds(double x,double y){
		double deltaX=x-x0;
		CoordinatesConverter coord=getCoord();
		GraphicNode node=(GraphicNode)selected;
		Rectangle2D bounds;
		double xStart=coord.toX((selectedIntervalNumber==0&&state==BAR_MOVE)?node.getStart():selectedInterval.getStart());
		if (state==PROGRESS_BAR_MOVE){
			double completedX=coord.toX(node.getCompleted());//CoordinatesConverter.adaptSmallBarEndX(xStart,coord.toX(node.getCompleted()),config);
			bounds=new Rectangle2D.Double(xStart,((GanttUI)ui).getBarY(node.getRow())+node.getGanttShapeOffset()+(node.getGanttShapeHeight()-config.getGanttProgressBarHeight())/2,completedX-xStart+deltaX,config.getGanttProgressBarHeight());
		}else{
			double xEnd=(selectedIntervalNumber==0&&state==BAR_MOVE)?CoordinatesConverter.adaptSmallBarEndX(coord.toX(node.getStart()),coord.toX(node.getEnd()),node,config):coord.toX(selectedInterval.getEnd());
			double w=xEnd-xStart;
			switch (state) {
			case BAR_MOVE:
				bounds=new Rectangle2D.Double(xStart+deltaX,((GanttUI)ui).getBarY(node.getRow())+node.getGanttShapeOffset(),w,node.getGanttShapeHeight());
				break;
			case BAR_MOVE_START:
				bounds=new Rectangle2D.Double(xStart+deltaX,((GanttUI)ui).getBarY(node.getRow())+node.getGanttShapeOffset(),w-deltaX,node.getGanttShapeHeight());
				break;
			case BAR_MOVE_END:
				bounds=new Rectangle2D.Double(xStart,((GanttUI)ui).getBarY(node.getRow())+node.getGanttShapeOffset(),w+deltaX,node.getGanttShapeHeight());
				break;
			default:
				return null;
			}
		}
		return bounds;
    }
    protected Rectangle2D getLinkSelectionShadowBounds(GraphicNode node){
		CoordinatesConverter coord=getCoord();
		double xStart=coord.toX(node.getStart());
		double xEnd=coord.toX(node.getEnd());
		xEnd=CoordinatesConverter.adaptSmallBarEndX(xStart,xEnd,node,config);
		Rectangle2D selectionRectangle=new Rectangle2D.Double(xStart,((GanttUI)ui).getBarY(node.getRow())+node.getGanttShapeOffset(),xEnd-xStart,node.getGanttShapeHeight());
		return selectionRectangle;
    }


    public CoordinatesConverter getCoord(){
    	return ((GanttUI)ui).getCoord();
    }

    protected void setLinkOrigin(){
    	GraphicNode node=(GraphicNode)selected;
		CoordinatesConverter coord=getCoord();
		double xStart=coord.toX((selectedIntervalNumber==0)?node.getStart():selectedInterval.getStart());
		double xEnd=selectedIntervalNumber==0?CoordinatesConverter.adaptSmallBarEndX(coord.toX(node.getStart()),coord.toX(node.getEnd()),node,config):coord.toX(selectedInterval.getEnd());
		x0link=(xStart+xEnd)/2;
		y0link=((GanttUI)ui).getBarY(node.getRow())+node.getGanttShapeOffset()+node.getGanttShapeHeight()/2;

    }

    protected boolean allowLinkSelectionToMove(){
    	return beforeLinkState==BAR_MOVE||beforeLinkState==BAR_MOVE_START||beforeLinkState==BAR_MOVE_END;
    }

    protected boolean switchOnLinkCreation(double x, double y){
    	if (state==PROGRESS_BAR_MOVE) return false;
		GraphicNode node=(GraphicNode)selected;
		Object impl = node.getNode().getImpl();
		return impl instanceof HasDependencies &&
				((int)y)/((Gantt)getGraph()).getRowHeight()!=node.getRow() ;
    }

    public Cursor selectCursor(){
    	Cursor cursor=null;
    	switch (state) {
		case BAR_MOVE_START:
			cursor=new Cursor(Cursor.W_RESIZE_CURSOR);
			break;
		case BAR_MOVE_END:
			cursor=new Cursor(Cursor.E_RESIZE_CURSOR);
			break;
		case PROGRESS_BAR_MOVE:
			cursor=getProgressCursor();
			break;
		case SPLIT:
			cursor=getSplitCursor();
			break;
		}
    	if (cursor==null) super.selectCursor();
    	else getGraph().setCursor(cursor);
    	updateHint();
    	return cursor;
    }

    //Plain-language help: a hover hint on bars and a live tip while dragging

    private String lastHint;
    /** Says what dragging here would do, as the chart's tooltip. */
    private void updateHint(){
    	String hint;
    	Task task=selectedTask();
    	switch (task==null&&state!=LINK_SELECTION?NOTHING_SELECTED:state) {
    	case BAR_MOVE: hint=Messages.getString(task.isWbsParent()?"GanttHint.summary":"GanttHint.move"); break;
    	case BAR_MOVE_START: hint=Messages.getString("GanttHint.start"); break;
    	case BAR_MOVE_END: hint=Messages.getString("GanttHint.end"); break;
    	case PROGRESS_BAR_MOVE: hint=Messages.getString("GanttHint.progress"); break;
    	case LINK_SELECTION: hint=Messages.getString("GanttHint.dependency"); break;
    	case SPLIT: hint=null; break;
    	default: hint=Messages.getString("Text.rightClickForOptions");
    	}
    	if (task!=null&&hint!=null) hint="<html><b>"+escape(task.getName())+"</b><br>"+hint+"</html>";
    	if (hint==null?lastHint==null:hint.equals(lastHint)) return;
    	lastHint=hint;
    	getGraph().setToolTipText(hint);
    }

    private Task selectedTask(){
    	if (!(selected instanceof GraphicNode)) return null;
    	Object impl=((GraphicNode)selected).getNode().getImpl();
    	return impl instanceof Task?(Task)impl:null;
    }

    private static String escape(String s){
    	return s==null?"":s.replace("&","&amp;").replace("<","&lt;");
    }

    /**
     * The time a drag to x moves the dragged edge by. When a pixel is more than an hour (day or coarser
     * zoom), the edge lands on a day boundary so bars don't end up starting at odd hours.
     */
    private long snappedDelta(double x){
    	long dt=(long)getCoord().toDuration(x-x0);
    	if (selectedInterval==null||getCoord().toDuration(1)<HOUR) return dt;
    	long edge=state==BAR_MOVE_END?selectedInterval.getEnd():selectedInterval.getStart();
    	if (state==BAR_MOVE&&selectedIntervalNumber==0&&selected instanceof GraphicNode) edge=((GraphicNode)selected).getStart();
    	long target=DateTime.dayFloor(edge+dt+DAY/2);
    	if (target==DateTime.dayFloor(edge+DAY/2)) return 0; // still the same day: no change, and no date pin
    	return target-edge;
    }
    private static final long HOUR=60L*60*1000;
    private static final long DAY=24*HOUR;

    /** What the drop at x would do: new dates, new length, new progress or the new dependency. */
    private String dragText(double x){
    	if (state==LINK_CREATION){
    		Task from=sourceNode==null||!(sourceNode.getNode().getImpl() instanceof Task)?null:(Task)sourceNode.getNode().getImpl();
    		Task to=destinationNode==null||!(destinationNode.getNode().getImpl() instanceof Task)?null:(Task)destinationNode.getNode().getImpl();
    		if (from==null) return null;
    		if (to==null) return "<html>"+Messages.getString("GanttHint.linkDrop")+"</html>";
    		return "<html>"+Messages.getString("GanttHint.linkWillStart").replace("{0}","<b>"+escape(to.getName())+"</b>").replace("{1}","<b>"+escape(from.getName())+"</b>")+"</html>";
    	}
    	if (!isMove()||!(selected instanceof GraphicNode)||selectedInterval==null) return null;
    	GraphicNode node=(GraphicNode)selected;
    	if (!(node.getNode().getImpl() instanceof Schedule)) return null;
    	WorkCalendar cal=((Schedule)node.getNode().getImpl()).getEffectiveWorkCalendar();
    	long dt=snappedDelta(x);
    	long start=selectedIntervalNumber==0&&state==BAR_MOVE?node.getStart():selectedInterval.getStart();
    	long end=selectedIntervalNumber==0&&state==BAR_MOVE?node.getEnd():selectedInterval.getEnd();
    	switch (state){
    	case BAR_MOVE:{
    		long work=cal.compare(end,start,false);
    		long s=cal.adjustInsideCalendar(start+dt,false);
    		return "<html>"+Messages.getString("GanttHint.dates").replace("{0}",DateRenderer.formatDay(s)).replace("{1}",DateRenderer.formatDay(cal.add(s,work,true)))+"</html>";
    	}
    	case BAR_MOVE_START:{
    		long s=Math.min(start+dt,end);
    		return "<html>"+Messages.getString("GanttHint.newStart").replace("{0}",DateRenderer.formatDay(cal.adjustInsideCalendar(s,false))).replace("{1}",DurationFormat.format(cal.compare(end,s,false)))+"</html>";
    	}
    	case BAR_MOVE_END:{
    		long e=Math.max(end+dt,start);
    		return "<html>"+Messages.getString("GanttHint.newEnd").replace("{0}",DateRenderer.formatDay(cal.adjustInsideCalendar(e,true))).replace("{1}",DurationFormat.format(cal.compare(e,start,false)))+"</html>";
    	}
    	case PROGRESS_BAR_MOVE:{
    		long total=cal.compare(node.getEnd(),node.getStart(),false);
    		long t=Math.max(node.getStart(),Math.min(node.getEnd(),(long)getCoord().toTime(x)));
    		int percent=total<=0?0:(int)Math.round(100.0*cal.compare(t,node.getStart(),false)/total);
    		return "<html>"+Messages.getString("GanttHint.percent").replace("{0}",String.valueOf(percent))+"</html>";
    	}
    	}
    	return null;
    }

    public void mouseDragged(MouseEvent e){
    	super.mouseDragged(e);
    	if (isReadOnly()||!SwingUtilities.isLeftMouseButton(e)) return;
    	DragTip.show(getGraph(),e.getX(),e.getY(),dragText(e.getX()));
    }

    public void mouseReleased(MouseEvent e){
    	DragTip.hide();
    	Task moved=state==BAR_MOVE||state==BAR_MOVE_START?selectedTask():null;
    	int constraintBefore=moved==null?ConstraintType.ASAP:moved.getConstraintType();
    	super.mouseReleased(e);
    	if (moved!=null&&constraintBefore==ConstraintType.ASAP&&moved.getConstraintType()!=ConstraintType.ASAP)
    		explainPin(moved);
    }

    private static boolean pinExplained=false;
    /** Moving a bar pins its start date; say so once in full, then briefly, and offer to undo it. */
    private void explainPin(final Task task){
    	String date=DateRenderer.formatDay(task.getConstraintDate());
    	String text=Messages.getString(pinExplained?"GanttHint.pinnedShort":"GanttHint.pinned").replace("{0}",task.getName()).replace("{1}",date);
    	pinExplained=true;
    	Toast.show(getGraph(),text,Messages.getString("GanttHint.unpin"),e->unpin(task),Messages.getString("GanttHint.undo"),e->GraphicManager.getInstance(getGraph()).doUndoRedo(true));
    }

    private void unpin(Task task){
    	NodeModel model=((Gantt)getGraph()).getModel().getCache().getModel();
    	com.projectlibre1.grouping.core.Node node=model.search(task);
    	if (node==null) return;
    	try {
    		model.setFieldValue(Configuration.getFieldFromId("Field.constraintType"),node,this,Integer.valueOf(ConstraintType.ASAP),null,NodeModel.NORMAL);
    	} catch (Exception ex) {
    		Alert.error(ex.getMessage(),getGraph());
    	}
    }

    public void mousePressed(MouseEvent e){
    	if (SwingUtilities.isRightMouseButton(e)&&!isReadOnly()){
    		select(e.getX(),e.getY());
    		SpreadSheet sheet=spreadSheet();
    		Task task=selectedTask();
    		if (task!=null&&sheet!=null&&selectRow(sheet,task)){
    			JPopupMenu menu=new TaskMenu(sheet).build();
    			((GanttPopupMenu)popup).init();
    			JMenu chart=new JMenu(Messages.getString("GanttHint.chartOptions"));
    			for (Component c:popup.getComponents()) chart.add(c);
    			menu.addSeparator();
    			menu.add(chart);
    			menu.show(getGraph(),e.getX(),e.getY());
    			return;
    		}
    	}
    	super.mousePressed(e);
    }

    private SpreadSheet spreadSheet(){
    	GanttView view=(GanttView)SwingUtilities.getAncestorOfClass(GanttView.class,getGraph());
    	return view==null?null:view.getSpreadSheet();
    }

    private static boolean selectRow(SpreadSheet sheet,Task task){
    	SpreadSheetModel model=(SpreadSheetModel)sheet.getModel();
    	for (int r=0;r<sheet.getRowCount();r++){
    		if (model.getNode(r).getNode().getImpl()==task){
    			sheet.finishCurrentOperations();
    			if (!sheet.isRowSelected(r)) sheet.changeSelection(r,1,false,false);
    			return true;
    		}
    	}
    	return false;
    }

    public boolean executeAction(double x,double y){
    	if ((x==x0&&state!=LINK_CREATION)||selected==null) return false; // a straight vertical drag still links
    	if (state==BAR_MOVE||state==BAR_MOVE_START||state==BAR_MOVE_END||state==PROGRESS_BAR_MOVE||state==SPLIT){
    		if (!(selected instanceof GraphicNode)) return false;
    		sourceNode=(GraphicNode)selected;
    	}
    	long t=(long)getCoord().toTime(x);
    	long dt=snappedDelta(x);
    	switch (state) {
		case BAR_MOVE:
			ScheduleService.getInstance().setInterval(this,(Schedule)sourceNode.getNode().getImpl(),selectedInterval.getStart()+dt,selectedInterval.getEnd()+dt,selectedInterval,ui.getGraph().getProject().getUndoController().getEditSupport());
			return true;
		case BAR_MOVE_START:
			ScheduleService.getInstance().setInterval(this,(Schedule)sourceNode.getNode().getImpl(),selectedInterval.getStart()+dt,selectedInterval.getEnd(),selectedInterval,ui.getGraph().getProject().getUndoController().getEditSupport());
			return true;
		case BAR_MOVE_END:
			ScheduleService.getInstance().setInterval(this,(Schedule)sourceNode.getNode().getImpl(),selectedInterval.getStart(),selectedInterval.getEnd()+dt,selectedInterval,ui.getGraph().getProject().getUndoController().getEditSupport());
			return true;
		case PROGRESS_BAR_MOVE:
			ScheduleService.getInstance().setCompleted(this,(Schedule)sourceNode.getNode().getImpl(),t,ui.getGraph().getProject().getUndoController().getEditSupport());
			return true;
		case LINK_CREATION:
			try {
					if (sourceNode!=null&&destinationNode!=null&&
							sourceNode.getNode().getImpl() instanceof HasDependencies &&
							destinationNode.getNode().getImpl() instanceof HasDependencies && !ClassUtils.isObjectReadOnly(destinationNode.getNode().getImpl())){
						DependencyService.getInstance().newDependency((HasDependencies)sourceNode.getNode().getImpl(),(HasDependencies)destinationNode.getNode().getImpl(),DependencyType.FS,0,this);
					}
				} catch (InvalidAssociationException e) {
					Alert.error(e.getMessage());
				}
				return true;
		case LINK_SELECTION:
			showDependencyPropertiesDialog((GraphicDependency)selected);
			return true;
		case SPLIT:
			ScheduleService.getInstance().split(this,(Schedule)sourceNode.getNode().getImpl(),t,t,ui.getGraph().getProject().getUndoController().getEditSupport());
			return true;
		}
    	return false;
    }

    public void setSplitMode(){
    	state=SPLIT;
    	selectCursor();
    }





    protected void select(int x,int y){
    	if (selection){
    		selectedZone=ui.getObjectAt(x,y);
    		selected=selectedZone==null?null:selectedZone.getObject();
    		if (state==SPLIT) return;
	    	if (selected==null ){
	    		state=NOTHING_SELECTED;
	    	}else{
	    		 findState(x,y);
	    	}
	    	selectCursor();
    	}
    }

    protected boolean isMove(){
    	return state==BAR_MOVE||state==BAR_MOVE_END||state==BAR_MOVE_START||state==PROGRESS_BAR_MOVE;
    }
    protected boolean isDirectAction(){
    	return state==SPLIT||super.isDirectAction();
    }
    protected boolean isRepaintOnRelease(){
    	return state==BAR_MOVE||state==BAR_MOVE_END||state==BAR_MOVE_START||state==PROGRESS_BAR_MOVE||state==LINK_CREATION;
    }

}
