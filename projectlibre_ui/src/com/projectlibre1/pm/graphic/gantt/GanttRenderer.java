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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;
import java.io.Serializable;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.ListIterator;

import javax.swing.CellRendererPane;
import javax.swing.JComponent;
import javax.swing.JLabel;

import org.apache.commons.collections.Closure;

import com.projectlibre1.pm.graphic.gantt.link_routing.GanttLinkRouting;
import com.projectlibre1.pm.graphic.graph.GraphParams;
import com.projectlibre1.pm.graphic.graph.GraphRenderer;
import com.projectlibre1.pm.graphic.graph.LinkRouting;
import com.projectlibre1.pm.graphic.laf.ModernLaf;
import com.projectlibre1.pm.graphic.model.cache.GraphicDependency;
import com.projectlibre1.pm.graphic.model.cache.GraphicNode;
import com.projectlibre1.pm.graphic.model.cache.NodeModelCache;
import com.projectlibre1.pm.graphic.timescale.CoordinatesConverter;
import com.projectlibre1.field.Field;
import com.projectlibre1.field.FieldConverter;
import com.projectlibre1.functor.IntervalConsumer;
import com.projectlibre1.functor.ScheduleIntervalGenerator;
import com.projectlibre1.graphic.configuration.BarFormat;
import com.projectlibre1.graphic.configuration.BarStyles;
import com.projectlibre1.graphic.configuration.GraphicConfiguration;
import com.projectlibre1.graphic.configuration.TexturedShape;
import com.projectlibre1.graphic.configuration.shape.PredefinedPaint;
import com.projectlibre1.options.GanttOption;
import com.projectlibre1.pm.calendar.CalendarService;
import com.projectlibre1.pm.calendar.WorkingCalendar;
import com.projectlibre1.pm.dependency.Dependency;
import com.projectlibre1.pm.dependency.DependencyType;
import com.projectlibre1.pm.scheduling.ScheduleInterval;
import com.projectlibre1.pm.task.Project;
import com.projectlibre1.strings.Messages;
import com.projectlibre1.timescale.CalendarUtil;
import com.projectlibre1.timescale.TimeInterval;
import com.projectlibre1.timescale.TimeIterator;
import com.projectlibre1.util.DateTime;
import com.projectlibre1.util.Environment;
import com.projectlibre1.util.FontUtil;

public class GanttRenderer extends GraphRenderer implements Serializable {
	/**
	 *
	 */
	private static final long serialVersionUID = -7437190083991277084L;
	protected NodeRenderer nodeRenderer = new NodeRenderer();
	protected LinkRenderer linkRenderer = new LinkRenderer();
	protected HorizontalLineRenderer horizontalLineRenderer = new HorizontalLineRenderer();
	protected AnnotationRenderer annotationRenderer = new AnnotationRenderer();

	protected GraphicConfiguration config;
	protected JComponent container;

	protected static final Stroke LINK_STROKE=new BasicStroke(1.2f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND);
	protected static final double LINK_CORNER_RADIUS=4;
	protected static final Stroke TODAY_STROKE=new BasicStroke(1.5f);
	protected static final Stroke PROJECT_START_STROKE=new BasicStroke(1f,BasicStroke.CAP_BUTT,BasicStroke.JOIN_MITER,10f,new float[]{4f,3f},0f);

	/** Progress is shown as a darker shade of the bar colour. */
	protected static Color progressColor(Color barColor){
		if (barColor==null) return ModernLaf.TEXT_PRIMARY;
		float f=0.72f;
		return new Color((int)(barColor.getRed()*f),(int)(barColor.getGreen()*f),(int)(barColor.getBlue()*f));
	}

	/** Copy of a polyline with its corners replaced by small quadratic curves. */
	protected static Shape roundCorners(GeneralPath path,double radius){
		ArrayList<double[]> points=new ArrayList<double[]>();
		double[] seg=new double[6];
		for (PathIterator i=path.getPathIterator(null);!i.isDone();i.next()){
			int type=i.currentSegment(seg);
			if (type!=PathIterator.SEG_MOVETO&&type!=PathIterator.SEG_LINETO) return path;
			points.add(new double[]{seg[0],seg[1]});
		}
		if (points.size()<3) return path;
		GeneralPath rounded=new GeneralPath();
		double[] p0=points.get(0);
		rounded.moveTo(p0[0],p0[1]);
		for (int k=1;k<points.size()-1;k++){
			double[] a=points.get(k-1),b=points.get(k),c=points.get(k+1);
			double d1=Math.hypot(b[0]-a[0],b[1]-a[1]),d2=Math.hypot(c[0]-b[0],c[1]-b[1]);
			double r=Math.min(radius,Math.min(d1,d2)/2);
			if (r<=0){
				rounded.lineTo(b[0],b[1]);
				continue;
			}
			rounded.lineTo(b[0]+(a[0]-b[0])*r/d1,b[1]+(a[1]-b[1])*r/d1);
			rounded.quadTo(b[0],b[1],b[0]+(c[0]-b[0])*r/d2,b[1]+(c[1]-b[1])*r/d2);
		}
		double[] last=points.get(points.size()-1);
		rounded.lineTo(last[0],last[1]);
		return rounded;
	}


	public GanttRenderer(){
		super();
		config=GraphicConfiguration.getInstance();
	}
	public GanttRenderer(GraphParams graphInfo){
		super(graphInfo);
		config=GraphicConfiguration.getInstance();
		if (graphInfo instanceof JComponent)
			container=(JComponent)graphInfo;
	}



	private class NodeRenderer implements Closure, IntervalConsumer, Serializable {
		private static final long serialVersionUID = -1348039741030744803L;
		GraphicNode node;
		Graphics2D g2;
		protected GanttBarSingleIntervalGenerator singleIntervalGenerator=new GanttBarSingleIntervalGenerator();
		protected ScheduleInterval interval;
		protected BarFormat format;
		protected int yrow;
		protected int maxLayer=Integer.MAX_VALUE;
		protected int minLayer=0;

		public void initialize(Graphics2D g2, GraphicNode node) {
			this.g2 = g2;
			this.node = node;
			int rowHeight=((GanttParams)graphInfo).getRowHeight();
			yrow=node.getRow()*rowHeight;
			setLayers(BarFormat.MIN_FOREGROUND_LAYER,BarFormat.MAX_FOREGROUND_LAYER);
		}

		public int getMaxLayer() {
			return maxLayer;
		}
		public void setMaxLayer(int maxLayer) {
			this.maxLayer = maxLayer;
		}

		public int getMinLayer() {
			return minLayer;
		}
		public void setMinLayer(int minLayer) {
			this.minLayer = minLayer;
		}

		public void setLayers(int minLayer,int maxLayer) {
			this.minLayer = minLayer;
			this.maxLayer = maxLayer;
		}

/**
 * This is the callback which is called from barStyles.apply() below
 */
		public void execute(Object arg0) {
			format = (BarFormat)arg0;
			if (format.getLayer()>maxLayer||format.getLayer()<minLayer) return;



		    ScheduleIntervalGenerator intervalGenerator;
			if (format.getScheduleIntervalGenerator()==null){
				singleIntervalGenerator.initialize(graphInfo.getCache().getModel(),format.getFromField(),format.getToField());
				intervalGenerator=singleIntervalGenerator;
			}else{
				intervalGenerator=format.getScheduleIntervalGenerator();
			}

			// collect first so segments that touch (e.g. split at the progress point) get flat joins
			intervalCount=0;
			intervalGenerator.consumeIntervals(node,collector);
			CoordinatesConverter coord=((GanttParams)graphInfo).getCoord();
			for (int k=0;k<intervalCount;k++){
				joinLeft=k>0&&coord.toX(intervalBounds[2*k])-coord.toX(intervalBounds[2*k-1])<=1;
				joinRight=k<intervalCount-1&&coord.toX(intervalBounds[2*k+2])-coord.toX(intervalBounds[2*k+1])<=1;
				consumeInterval(new ScheduleInterval(intervalBounds[2*k],intervalBounds[2*k+1]));
			}
			joinLeft=joinRight=false;

		}

		protected long[] intervalBounds=new long[16];
		protected int intervalCount;
		protected boolean joinLeft,joinRight;
		protected IntervalConsumer collector=new IntervalConsumer(){
			public void consumeInterval(ScheduleInterval interval){
				if (2*intervalCount+1>=intervalBounds.length){
					long[] bigger=new long[intervalBounds.length*2];
					System.arraycopy(intervalBounds,0,bigger,0,intervalBounds.length);
					intervalBounds=bigger;
				}
				intervalBounds[2*intervalCount]=interval.getStart();
				intervalBounds[2*intervalCount+1]=interval.getEnd();
				intervalCount++;
			}
		};



		public void consumeInterval(ScheduleInterval interval){
//			System.out.println("GanttUI consuming interval " + new java.util.Date(interval.getStart()) + " " + new java.util.Date(interval.getEnd()));
//			if (interval.getEnd() < interval.getStart())
//				return;
			CoordinatesConverter coord=((GanttParams)graphInfo).getCoord();
			if (interval.getEnd()>100000000000000L){
				// this hasn't happened in years. whatever caused it is fixed, but keeping just in case
				System.out.println("ERROR!!! leads to OutOfMemoryError, consumeInterval interval="+interval.getStart()+", "+CalendarUtil.toString(interval.getStart())+", "+interval.getEnd()+", "+CalendarUtil.toString(interval.getEnd())+"...");
				return;
			}
			double x=coord.toX(interval.getStart());
			double width=CoordinatesConverter.adaptSmallBarEndX(x,coord.toX(interval.getEnd()),node,config)-x;
//			double width=coord.toW(interval.getEnd()-interval.getStart());
			double height;
			double y=yrow+config.getGanttBarYOffset();
			int row=format.getRow();
		    if (row==1){
		    	height=config.getGanttBarHeight();
		    }
		    else{
		    	height=config.getBaselineHeight();
			    y+=config.getGanttBarHeight()+config.getBaselineHeight()*(row-2);
		    }
	    	y+=height/2;

			double dw=height;
			Shape barShape=null;

			if (format.getMiddle()!=null){

				if (g2==null&&format.isMain()){
					Shape shape=format.getMiddle().toGeneralPath(
							width,
							height,
							x,
							y,
							null);
					Rectangle2D bounds=shape.getBounds2D();
					node.setGanttShapeOffset(bounds.getY()-y+height/2);
					node.setGanttShapeHeight(bounds.getHeight());
				}else{
					barShape=format.getMiddle().draw(g2,
							width,
							height,
							x,
							y,
							useTextures());
					if ((joinLeft||joinRight)&&barShape!=null){
						// square off the rounded ends where this segment touches its neighbour
						Rectangle2D b=barShape.getBounds2D();
						double r=Math.min(b.getHeight()/2,b.getWidth()/2);
						Area joined=new Area(barShape);
						if (joinLeft) joined.add(new Area(new Rectangle2D.Double(b.getX(),b.getY(),r,b.getHeight())));
						if (joinRight) joined.add(new Area(new Rectangle2D.Double(b.getMaxX()-r,b.getY(),r+1,b.getHeight())));
						format.getMiddle().paintShape(g2,joined,useTextures());
						barShape=joined;
					}

				}
				// draw middle before ends
			}
			if (g2==null) return;

			if (format.getStart()!=null) format.getStart().draw(g2,
					dw,
					height,
					x ,
					y,
					useTextures());
			if (format.getEnd()!=null) format.getEnd().draw(g2,
					dw,
					height,
					x+width,
					y,
					useTextures()); //TODO case when no start symbol

			//TODO style and format of completion should be treated with bar prefererences instead of a special case
			if (format.isMain()&&!node.isSummary()&&node.isStarted()){
				long completedT=node.getCompleted();
				if (completedT>=interval.getStart()){
					double completedW=coord.toX(completedT)-x;
					if (completedW>width && !GanttOption.getInstance().isCompletionIsContiguous())
						completedW=width;
					completedW=CoordinatesConverter.adaptSmallBarEndX(x, x+completedW, node,config)-x;
					// progress: darker fill inside the bar, clipped to its rounded outline
					Rectangle2D progressBar=new Rectangle2D.Double(x,y-height/2,completedW,height);
					Shape oldClip=g2.getClip();
					if (barShape!=null) g2.clip(barShape);
					g2.setColor(progressColor(format.getMiddle()==null?null:format.getMiddle().getColor()));
					g2.fill(progressBar);
					g2.setClip(oldClip);
				}
			}


		}

	}

	private class AnnotationRenderer implements Closure, Serializable {
		private static final long serialVersionUID = -137778741030744803L;
		protected BarFormat format;
		GraphicNode node;
		Graphics2D g2;
		protected int yrow;
		protected JLabel component=new JLabel();
		protected CellRendererPane rendererPane;
		FontMetrics fontMetrics;

		public void initialize(Graphics2D g2, GraphicNode node) {
			this.g2 = g2;
			this.node = node;
			int rowHeight=((GanttParams)graphInfo).getRowHeight();
			config=((GanttParams)graphInfo).getConfiguration();
			yrow=node.getRow()*rowHeight;

			if (container!=null){
				rendererPane=new CellRendererPane();
				container.add(rendererPane);
			}
			component.setFont(FontUtil.getFont(null,Environment.GANTT_ANNOTATIONS_FONT));
			fontMetrics=component.getFontMetrics(component.getFont());
		}

		public void execute(Object arg0) {
			format = (BarFormat)arg0;
			Field field=format.getField();
			if (field==null) return;
			Object value=field.getValue(node.getNode(),graphInfo.getCache().getModel(),null);
			if (value==null) return;
			CoordinatesConverter coord=((GanttParams)graphInfo).getCoord();

//			int y=yrow+config.getGanttBarHeight()+config.getGanttBarYOffset();
//			int x=(int)Math.ceil(coord.toX(node.getEnd()))+config.getGanttBarAnnotationXOffset();
//			Color oldColor=g2.getColor();
//			g2.setColor(format.getMiddle().getColor());
//			g2.drawString(ObjectConverterManager.toString(value,value.getClass()), x, y);
//			if (oldColor!=null) g2.setColor(oldColor);
			String s;
			if (value instanceof Date){
				Date d=(Date)value;
				s=DateFormat.getDateInstance(DateFormat.SHORT).format(d);
				int i=s.lastIndexOf('/');
				if (i>0) s=s.substring(0, i);
			}
			else s=FieldConverter.toString(value,value.getClass(),null);
			component.setText(s); //field.getClazz()?
			int y=yrow+config.getGanttBarYOffset();//+config.getGanttBarAnnotationYOffset();
			double x0=coord.toX(node.getStart());
			double x1=coord.toX(node.getEnd());
			x1=CoordinatesConverter.adaptSmallBarEndX(x0,x1,node,config);

			int x=(int)Math.ceil(x1)+config.getGanttBarAnnotationXOffset();
			int w=fontMetrics.stringWidth(s);//config.getGanttBarAnnotationMaxWidth();
			int h=config.getGanttBarHeight();

			if (container==null){
		    	component.setDoubleBuffered(false);
		    	component.setOpaque(false);
		    	component.setForeground(format.getMiddle().getColor());
		    	component.setSize(w,h);
		    	g2.translate(x,y);
		    	component.doLayout();
		    	component.print(g2);
		    	g2.translate(-x,-y);
			}
			else {
				component.setForeground(format.getMiddle().getColor());
				rendererPane.paintComponent(g2,component,container,x,y,w,h,true);
			}



		}



	}

	private class HorizontalLineRenderer implements Closure, Serializable {
		private static final long serialVersionUID = -6350307720624037262L;
		protected BarFormat format;
		GraphicNode node;
		Graphics2D g2;
		protected int yrow;

		public void initialize(Graphics2D g2, GraphicNode node) {
			this.g2 = g2;
			this.node = node;
			int rowHeight=((GanttParams)graphInfo).getRowHeight();
			config=((GanttParams)graphInfo).getConfiguration();
			yrow=(node.getRow()+1)*rowHeight -1; // draws under each row

		}

		public void execute(Object arg0) {
			format = (BarFormat)arg0;
			Rectangle bounds = g2.getClipBounds();
			Stroke oldStroke = g2.getStroke();
			Color oldColor = g2.getColor();
			g2.setColor(format.getMiddle().getColor());
			g2.drawLine(bounds.x,yrow,bounds.x+bounds.width,yrow);
			g2.setColor(oldColor);
		}
	}


	private class LinkRenderer implements Closure, Serializable {
		private static final long serialVersionUID = -2031158189787837110L;
		protected BarFormat format;
		protected GraphicDependency dependency;
		protected Graphics2D g2;
		void initialize(Graphics2D g2, GraphicDependency dependency) {
			this.g2 = g2;
			this.dependency = dependency;
		}


		private double[] extraPoints=new double[3];
		public void execute(Object arg0) {
			format = (BarFormat)arg0;

			GanttLinkRouting routing=(GanttLinkRouting)((GanttParams)graphInfo).getRouting();
			CoordinatesConverter coord=((GanttParams)graphInfo).getCoord();
			//if (format.getMiddle()!=null){
			    GraphicNode from=dependency.getPredecessor();
			    GraphicNode to=dependency.getSuccessor();
			    int type=dependency.getType();
				int fromSign=(type==DependencyType.SF||type==DependencyType.SS)?-1:1;
				int toSign=(type==DependencyType.FS||type==DependencyType.SS)?-1:1;
				double fx0=coord.toX(from.getStart());
				double fx1=coord.toX(from.getEnd());
				fx1=CoordinatesConverter.adaptSmallBarEndX(fx0,fx1,from,config);
				double tx0=coord.toX(to.getStart());
				double tx1=coord.toX(to.getEnd());
				tx1=CoordinatesConverter.adaptSmallBarEndX(tx0,tx1,to,config);
				double x0=fromSign<0?fx0:fx1;
				double x1=toSign<0?tx0:tx1;
				int rowHeight=((GanttParams)graphInfo).getRowHeight();
				int yOffset=config.getGanttBarYOffset()+config.getGanttBarHeight()/2;
				int y0=rowHeight*from.getRow();
				int y1=rowHeight*to.getRow();
				double y2=Math.max(y0,y1);
				y0+=yOffset;
				y1+=yOffset;

				GeneralPath path=dependency.getPath();
				((GanttLinkRouting)routing).routePath(path,x0,y0,x1,y1,y2,y1+to.getGanttShapeHeight()/2,y1-to.getGanttShapeHeight()/2,type);



				Color oldColor=g2.getColor();
				Stroke oldStroke = g2.getStroke();
				Dependency dep = dependency.getDependency();
				if (dep.isDisabled()) g2.setStroke(DISABLED_LINK_STROKE);
				if (dep.isCrossProject()) g2.setColor(/*dep.isDirty()?Color.ORANGE:*/EXTERNAL_LINK_COLOR);
				else g2.setColor(/*dep.isDirty()?Color.RED:*/format.getMiddle().getColor());
				if (!dep.isDisabled()) g2.setStroke(LINK_STROKE);
				g2.draw(roundCorners(path,LINK_CORNER_RADIUS));

			//}
			if (format.getStart()==null&&format.getEnd()==null) return;
			if (format.getStart()!=null){
				double theta=routing.getFirstAngle();
				AffineTransform transform=(theta==0)?null:AffineTransform.getRotateInstance(theta,routing.getFirstX(),routing.getFirstY());
				drawLinkArrows(dep,transform,format.getStart());
			}
			if (format.getEnd()!=null){
				double theta=routing.getLastAngle();
				AffineTransform transform=(theta==Math.PI||theta==-Math.PI)?null:AffineTransform.getRotateInstance(Math.PI-theta,routing.getLastX(),routing.getLastY());
				drawLinkArrows(dep,transform,format.getEnd());
			}

			if (oldColor!=null) g2.setColor(oldColor);
			if (oldStroke!= null) g2.setStroke(oldStroke);
		}

		private void drawLinkArrows(Dependency dep, AffineTransform transform, TexturedShape shape) {
			Color oldEndColor = format.getEnd().getColor();
			if (dep.isCrossProject())
				shape.setPaint(EXTERNAL_LINK_COLOR);
			g2.setColor(shape.getColor());
			LinkRouting routing=((GanttParams)graphInfo).getRouting();
			shape.draw(g2,routing.getLastX(),routing.getLastY(),transform,useTextures());
			if (dep.isCrossProject())
				shape.setPaint(oldEndColor);
		}
	}


    public void updateShapes(ListIterator nodeIterator){

    	Rectangle bounds = ((GanttParams)graphInfo).getGanttBounds();
    	CoordinatesConverter coord=((GanttParams)graphInfo).getCoord();
    	if (coord==null) return;
		double rowHeight=((GanttParams)graphInfo).getRowHeight();

		int i0=(int)Math.floor(bounds.getY()/rowHeight);
		int i1=(int)Math.ceil(bounds.getMaxY()/rowHeight);
		double t0=coord.toTime(bounds.getX());
		double t1=coord.toTime(bounds.getMaxX());

		GraphicNode node;
		for (ListIterator i=nodeIterator;i.hasNext();){
			node=(GraphicNode)i.next();
			node.setRow(i.previousIndex());
			if (i.previousIndex()>=i0&&i.previousIndex()<i1){
				if (!node.isVoid()) updateShape(node);
			}
		}
    }

    public void updateShape(GraphicNode node){
    	if (((GanttParams)graphInfo).getCoord()==null) return; //not initialized
		BarStyles barStyles = graphInfo.getBarStyles();
		nodeRenderer.initialize(null,node);
		barStyles.apply(node.getNode().getImpl(),nodeRenderer);

    }

	public void paintNode(Graphics2D g2,GraphicNode node, boolean background){
		BarStyles barStyles = graphInfo.getBarStyles();
		nodeRenderer.initialize(g2,node);

		if (background)
			nodeRenderer.setLayers(BarFormat.MIN_BACKGROUND_LAYER,BarFormat.MAX_BACKGROUND_LAYER);
		else nodeRenderer.setLayers(BarFormat.MIN_FOREGROUND_LAYER,BarFormat.MAX_FOREGROUND_LAYER);
		barStyles.apply(node.getNode().getImpl(),nodeRenderer);

	}

	public void paintAnnotation(Graphics2D g2,GraphicNode node){
		BarStyles barStyles = graphInfo.getBarStyles();
		annotationRenderer.initialize(g2,node);
		barStyles.apply(node.getNode().getImpl(),annotationRenderer,false,true,false, false);
	}

	public void paintHorizontalLine(Graphics2D g2,GraphicNode node){
		BarStyles barStyles = graphInfo.getBarStyles();
		horizontalLineRenderer.initialize(g2,node);
		barStyles.apply(node.getNode().getImpl(),horizontalLineRenderer,false,false,false, true);
	}

	public void paintLink(Graphics2D g2, GraphicDependency dependency){
		BarStyles barStyles = graphInfo.getBarStyles();
		linkRenderer.initialize(g2,dependency);
		barStyles.apply(dependency,linkRenderer,true,false,false, false);
	}


	protected BarFormat calendarFormat;
	protected Closure calendarClosure=new Closure(){
		public void execute(Object arg0) {
			calendarFormat = (BarFormat)arg0;
		}
	};
	protected BarFormat getCalendarFormat(){
		calendarFormat=null;
		if (calendarFormat==null){
			BarStyles barStyles = graphInfo.getBarStyles();
			barStyles.apply(null,calendarClosure,false,false,true, false);
		}
		return calendarFormat;
	}

	public void paintNonWorkingDays(Graphics2D g2,Rectangle bounds){
		BarFormat calFormat=getCalendarFormat();
		if (calFormat==null) return;
		//non working days
			Color oldColor=g2.getColor();
			Paint oldPaint=g2.getPaint();
			CoordinatesConverter coord=((GanttParams)graphInfo).getCoord();
			Project project=coord.getProject();
			WorkingCalendar wc=(WorkingCalendar)project.getWorkCalendar();

			if (coord.getTimescaleManager().isShowWholeDays()){
				boolean useScale2=coord.getTimescaleManager().getCurrentScaleIndex()==0; //valid only for current time scales
				TimeIterator i=coord.getTimeIterator(bounds.getX(), bounds.getMaxX(),useScale2);
				long startNonworking=-1L,endNonWorking=-1L;
				Calendar cal=DateTime.calendarInstance();

				PredefinedPaint paint=(PredefinedPaint)calFormat.getMiddle().getPaint();//new PredefinedPaint(PredefinedPaint.DOT_LINE,Colors.VERY_LIGHT_GRAY,Color.WHITE);
				paint.applyPaint(g2, useTextures());
				while (i.hasNext()){
					TimeInterval interval=i.next();
					long s=interval.getStart();
					if (CalendarService.getInstance().getDay(wc, s).isWorking()){
						if (startNonworking!=-1L){
							drawNonWorking(g2, startNonworking, endNonWorking, cal, coord, bounds,useScale2);
							startNonworking=endNonWorking=-1L;
						}
					}else{
						if (startNonworking==-1L) startNonworking=s;
						endNonWorking=s;

					}
				}
				if (startNonworking!=-1L){
					drawNonWorking(g2, startNonworking, endNonWorking, cal, coord, bounds,useScale2);
					startNonworking=endNonWorking=-1L;
				}
			}else if (coord.toW(DAY_MILLIS)>=4){
				// week scales: still shade weekends day by day while days are wide enough to see
				PredefinedPaint paint=(PredefinedPaint)calFormat.getMiddle().getPaint();
				paint.applyPaint(g2, useTextures());
				long end=(long)coord.toTime(bounds.getMaxX());
				long day=DateTime.dayFloor((long)coord.toTime(bounds.getX()));
				long startNonworking=-1L;
				while (day<=end){
					long next=DateTime.nextDay(day);
					if (CalendarService.getInstance().getDay(wc, day).isWorking()){
						if (startNonworking!=-1L){
							fillNonWorking(g2, startNonworking, day, coord, bounds);
							startNonworking=-1L;
						}
					}else if (startNonworking==-1L) startNonworking=day;
					day=next;
				}
				if (startNonworking!=-1L) fillNonWorking(g2, startNonworking, day, coord, bounds);
			}

		if (container!=null){

//Slow with with Java >6
//			//scale2 separation lines
//			TimeIterator i=coord.getTimeIterator(bounds.getX(), bounds.getMaxX(),true);
//			g2.setPaint(new PredefinedPaint(PredefinedPaint.DOT_LINE2,Color.GRAY,g2.getBackground()));
//			while (i.hasNext()){
//				TimeInterval interval=i.next();
//				int startX=(int)Math.round(coord.toX(interval.getStart()));
//				g2.drawLine(startX,bounds.y,startX,bounds.y+bounds.height);
//			}
			Stroke oldStroke=g2.getStroke();

			//project start
			long now=DateTime.gmt(new Date());
			int projectStartX=(int)Math.round(coord.toX(project.getStart()));
			if (projectStartX>=bounds.getX()&&projectStartX<=bounds.getMaxX()&&Math.abs(projectStartX-coord.toX(now))>8){
				g2.setColor(ModernLaf.TEXT_SECONDARY.brighter());
				g2.setStroke(PROJECT_START_STROKE);
				g2.drawLine(projectStartX,bounds.y,projectStartX,bounds.y+bounds.height);
			}

			//status date
			long statusDate = project.getStatusDate();
			if (statusDate != 0 && DateTime.dayFloor(statusDate)!=DateTime.dayFloor(now)) { // today has its own line
				int statusDateX=(int)Math.round(coord.toX(statusDate));
				if (statusDateX>=bounds.getX()&&statusDateX<=bounds.getMaxX()){
					g2.setColor(new Color(0x2F9E6E));
					g2.setStroke(PROJECT_START_STROKE);
					g2.drawLine(statusDateX,bounds.y,statusDateX,bounds.y+bounds.height);
				}
			}

			//today
			double todayX=coord.toX(now);
			if (todayX>=bounds.getX()&&todayX<=bounds.getMaxX()){
				g2.setColor(ModernLaf.TODAY);
				g2.setStroke(TODAY_STROKE);
				g2.draw(new java.awt.geom.Line2D.Double(todayX,bounds.y,todayX,bounds.y+bounds.height));
			}
			g2.setStroke(oldStroke);


			if (oldColor!=null) g2.setColor(oldColor);
			if (oldPaint!=null) g2.setPaint(oldPaint);

		}
	}

	protected static final long DAY_MILLIS=24L*60*60*1000;
	private void fillNonWorking(Graphics2D g2,long start,long end,CoordinatesConverter coord, Rectangle bounds){
		int x0=(int)Math.round(coord.toX(start));
		int x1=(int)Math.round(coord.toX(end));
		g2.fillRect(x0, bounds.y, x1-x0, bounds.height);
	}

	private void drawNonWorking(Graphics2D g2,long startNonworking,long endNonWorking, Calendar cal,CoordinatesConverter coord, Rectangle bounds,boolean userScale2){
		cal.setTimeInMillis(endNonWorking);
		if (userScale2) coord.getTimescaleManager().getScale().increment2(cal);
		else coord.getTimescaleManager().getScale().increment1(cal);
		endNonWorking=cal.getTimeInMillis();
		g2.fillRect((int)Math.round(coord.toX(startNonworking)), bounds.y, (int)Math.round(coord.toW(endNonWorking-startNonworking)), bounds.height);
	}


	ArrayList nodeList=new ArrayList();
    public void paint(Graphics g) {
    	paint(g,null);
    }
    public void paint(Graphics g,Rectangle visibleBounds) {
		Graphics2D g2=(Graphics2D)g;
    	//CoordinatesConverter coord=((GanttParams)graphInfo).getCoord();

		Rectangle clipBounds = g2.getClipBounds();
		Rectangle svgClip=clipBounds;
		if (clipBounds==null){
			clipBounds=((GanttParams)getGraphInfo()).getGanttBounds();
			//start at O,O because it's already translated
			if (visibleBounds==null) clipBounds=new Rectangle(0,1,clipBounds.width,clipBounds.height-2);//1 pixel offset needed for edge
//			else clipBounds=new Rectangle(visibleBounds.x-clipBounds.x,visibleBounds.y-clipBounds.y,visibleBounds.width,visibleBounds.height);
			else {
				clipBounds=visibleBounds;
				g2.setClip(clipBounds);
			}
		}

		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		paintNonWorkingDays(g2,clipBounds);

		//Modif for offline graphics

		double rowHeight=((GanttParams)graphInfo).getRowHeight();

		int i0=(int)Math.floor(clipBounds.getY()/rowHeight);
		int i1;
		if (visibleBounds==null) i1=(int)Math.ceil(clipBounds.getMaxY()/rowHeight);
		else i1=(int)Math.floor(clipBounds.getMaxY()/rowHeight);
		//double t0=coord.toTime(clipBounds.getX());
		//double t1=coord.toTime(clipBounds.getMaxX());

		nodeList.clear();

		GraphicNode node;
//		for (ListIterator i=graph.getModel().getNodeIterator(i0);i.hasNext()&&i.nextIndex()<=i1;){
//			node=(GraphicNode)i.next();
//			if (!node.isSchedule()) continue;
//			nodeList.add(node);
//			node.setRow(i.previousIndex());
//			paintNode(g2,node,true);
//		} //Because row not initialized for some nodes

		NodeModelCache cache=graphInfo.getCache();
		for (ListIterator i=cache.getIterator();i.hasNext();){
			node=(GraphicNode)i.next();
			node.setRow(i.previousIndex());
			if (i.previousIndex()>=i0&&i.previousIndex()<i1){
				if (!node.isSchedule()) continue;
				nodeList.add(node);
				paintAnnotation(g2,node);
				paintNode(g2,node,true);
				paintHorizontalLine(g2,node);
			}
		}

		GraphicDependency dependency;
		for (Iterator i=cache.getEdgesIterator();i.hasNext();){
			dependency=(GraphicDependency)i.next();
			//if (nodeList.contains(dependency.getPredecessor())||nodeList.contains(dependency.getSuccessor()))
				paintLink(g2,dependency);
		}

		for (ListIterator i=nodeList.listIterator();i.hasNext();){
			node=(GraphicNode)i.next();
			paintNode(g2,node,false);
		}

		if (visibleBounds!=null) g2.setClip(svgClip);
		else if (container!=null&&nodeList.isEmpty()&&isEmptyProject(cache)) paintEmptyHint(g2);

	}

	protected boolean isEmptyProject(NodeModelCache cache){
		for (ListIterator i=cache.getIterator();i.hasNext();){
			if (!((GraphicNode)i.next()).isVoid()) return false;
		}
		return true;
	}

	/** Centered hint in the chart of a project without tasks. */
	protected void paintEmptyHint(Graphics2D g2){
		Rectangle visible=container.getVisibleRect();
		String text=Messages.getString("Text.emptyGanttHint");
		Font font=container.getFont();
		if (font==null) return;
		g2.setFont(font.deriveFont(font.getSize2D()+1f));
		FontMetrics fm=g2.getFontMetrics();
		g2.setColor(ModernLaf.TEXT_SECONDARY);
		g2.drawString(text,visible.x+(visible.width-fm.stringWidth(text))/2,visible.y+visible.height/3);
	}



}
