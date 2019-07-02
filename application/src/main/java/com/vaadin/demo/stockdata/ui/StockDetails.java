package com.vaadin.demo.stockdata.ui;

import com.vaadin.demo.stockdata.backend.db.demodata.stockdata.data_point.DataPoint;
import com.vaadin.demo.stockdata.backend.db.demodata.stockdata.symbol.Symbol;
import com.vaadin.demo.stockdata.backend.service.Service;
import com.vaadin.demo.stockdata.ui.components.StockChart;
import com.vaadin.demo.stockdata.ui.util.MoneyFormatter;
import com.vaadin.demo.stockdata.ui.util.ServiceDirectory;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.charts.events.XAxesExtremesSetEvent;
import com.vaadin.flow.component.charts.model.DataSeries;
import com.vaadin.flow.component.charts.model.DataSeriesItem;
import com.vaadin.flow.component.charts.model.OhlcItem;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;


import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

@StyleSheet("frontend://styles/stock-details.css")
public class StockDetails extends VerticalLayout implements StockList.SymbolSelectedListener {

  /**
   * Approximate number of data points returned in each batch
   */
  private static final int DATA_POINT_BATCH_SIZE = 300;

  private Service service = ServiceDirectory.getServiceInstance();


  StockDetails() {
    addClassName("stock-details");
    setSizeFull();
    setSpacing(true);

    showNoSymbolSelected();
  }

  private void showNoSymbolSelected() {
    Span noSymbolSelected = new Span("No symbol selected");
    noSymbolSelected.getStyle().set("align-self", "center");
    add(noSymbolSelected);
  }

  @Override
  public void symbolSelected(Symbol symbol) {
    removeAll();

    if (symbol != null) {
      addSymbolDetailsLayout(symbol);
      addDetailChart(symbol);
    } else {
      showNoSymbolSelected();
    }
  }

  private void addSymbolDetailsLayout(Symbol symbol) {
    service.getMostRecentDataPoint(symbol).ifPresent(dataPoint -> {

      Span currentValue = new Span(MoneyFormatter.format(dataPoint.getClose()));
      Div ticker = new Div(new Text(symbol.getTicker()));
      Div name = new Div(new Text(symbol.getName()));
      Div companyInfo = new Div(ticker, name);

      currentValue.addClassName("current-value");
      ticker.addClassName("ticker");
      name.addClassName("name");
      companyInfo.addClassName("company-info");

      FlexLayout flexLayout = new FlexLayout(currentValue, companyInfo);
      flexLayout.setWidth("100%");
      flexLayout.setJustifyContentMode(JustifyContentMode.BETWEEN);
      flexLayout.setAlignItems(Alignment.CENTER);
      add(flexLayout);
    });
  }


  private List<DataSeriesItem> getSymbolData(Symbol symbol, LocalDateTime startDate, LocalDateTime endDate) {
    return service.getHistoryData(symbol, startDate, endDate, DATA_POINT_BATCH_SIZE)
        .map(this::toChartItem)
        .collect(Collectors.toList());
  }

  private OhlcItem toChartItem(DataPoint dataPoint) {
    OhlcItem ohlcItem = new OhlcItem();
    ohlcItem.setOpen(dataPoint.getOpen() / 100.0);
    ohlcItem.setHigh(dataPoint.getHigh() / 100.0);
    ohlcItem.setLow(dataPoint.getLow() / 100.0);
    ohlcItem.setClose(dataPoint.getClose() / 100.0);
    ohlcItem.setX(Instant.ofEpochSecond(dataPoint.getTimeStamp()));
    return ohlcItem;
  }

  private void addDetailChart(Symbol symbol) {
    StockChart chart = new StockChart();

    DataSeries dataSeries = new DataSeries();
    dataSeries.setName("Value");
    dataSeries.setData(getSymbolData(symbol, LocalDateTime.MIN, LocalDateTime.MAX));
    chart.getConfiguration().setSeries(dataSeries);


    //Use ComponentUtil to debounce the events - no need to hit the db on each
    ComponentUtil.addListener(chart, XAxesExtremesSetEvent.class, event -> {
          List<DataSeriesItem> zoomedData = getSymbolData(symbol,
              toLocalDateTime(event.getMinimum()),
              toLocalDateTime(event.getMaximum()));
          dataSeries.setData(zoomedData);
          getUI().ifPresent(ui -> ui.access(dataSeries::updateSeries));
        },
        r -> r.debounce(500));

    add(chart);
    expand(chart);
  }


  private LocalDateTime toLocalDateTime(Double jsTimestamp) {
    return Instant.ofEpochMilli(jsTimestamp.longValue()).atZone(ZoneId.systemDefault()).toLocalDateTime();
  }


}
