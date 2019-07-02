package com.vaadin.demo.stockdata.ui;

import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.page.BodySize;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.component.page.Viewport;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.Theme;
import com.vaadin.flow.theme.lumo.Lumo;

@Push
@Route("")
public class MainView extends HorizontalLayout {

  public MainView() {
    setSizeFull();
    setSpacing(true);

    StockList stockList = new StockList();
    stockList.getElement().getStyle().set("flex-shrink", "0");
    StockDetails stockDetails = new StockDetails();
    stockList.addSelectedListener(stockDetails);

    add(stockList, stockDetails);
    expand(stockDetails);
  }

}