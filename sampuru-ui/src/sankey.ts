/// <reference types="d3" />
/// <reference types="d3-sankey" />

import * as d3 from "d3";
import * as d3_sankey from "d3-sankey";
import {SankeyTransition} from "./data-transfer-objects";
import {node} from "webpack";

export interface SankeyData {
  source: string | number,
  target: string | number,
  value: number
}

export interface Node {
  name: string,
  index: number
}

export interface Link {
  value: number
}

/**
 * Return formatted source and target node names, and value
 * */
export function linkSetup(key: keyof SankeyTransition, sankey: SankeyTransition): SankeyData {
  switch(key) {
    case "receipt":
      return { source: "Receipt", target: "Extraction", value: sankey[key].extraction };
    case "extraction":
      return { source: "Extraction", target: "Library Preparation", value: sankey[key].library_preparation };
    case "library_preparation":
      return { source: "Library Preparation", target: "Low-Pass Sequencing", value: sankey[key].low_pass_sequencing };
    case "low_pass_sequencing":
      return { source: "Low-Pass Sequencing", target: "Full-Depth Sequencing", value: sankey[key].full_depth_sequencing };
    case "full_depth_sequencing":
      return { source: "Full-Depth Sequencing", target: "Informatics Interpretation", value: sankey[key].informatics_interpretation };
    case "informatics_interpretation":
      return { source: "Informatics Interpretation", target: "Final Report", value: sankey[key].final_report };
    case "final_report":
      return { source: "Final Report", target: "Passed", value: sankey[key].passed };
  }
}

/**
 * d3 will need a csv with data in the format:
 * source,target,value
 * In order to construct nodes and links
 * */
export function preprocess(sankey: SankeyTransition): SankeyData[] {
  const data: SankeyData[] = [];

  // set up links between quality gates, and pending, failed
  (Object.keys(sankey) as Array<keyof SankeyTransition>).forEach(
    (key, index, keyArray) => {
      const link = linkSetup(key, sankey);

      if (link.value > 0) {
        data.push(
          {
            source: link.source,
            target: link.target,
            value: link.value
          });
      }

      if (sankey[key].pending > 0) {
        data.push(
          {
            source: link.source,
            target: "Pending",
            value: sankey[key].pending
          });
      }

      if (sankey[key].failed > 0) {
        data.push(
          {
            source: link.source,
            target: "Failed",
            value: sankey[key].failed
          });
      }
    });

  data.forEach((d) => console.log("preprocess: " + d.source));
  return data;
}

/**
 * Create svg object from SankeyTransition data
 * Use targetId to know which element to append the svg element to
 * */
export function sankeyPlot(sankey: SankeyTransition, targetId: string) {
  const data = preprocess(sankey);

  // set the dimensions and margins of the graph
  const margin = {top: 10, right: 10, bottom: 10, left: 10},
    width = 900 - margin.left - margin.right,
    height = 300 - margin.top - margin.bottom;

  // format variables
  const formatNumber = d3.format(",.0f"),    // zero decimal places
    format = function (d: number | { valueOf(): number; }) {
      return formatNumber(d);
    },
    color = d3.scaleOrdinal(d3.schemeCategory10);

  // append the svg object to the body of the page
  const svg = d3.select("body").select("div").select(".container .card .card-body")
    .select(targetId).append("svg")
    .attr("width", width + margin.left + margin.right)
    .attr("height", height + margin.top + margin.bottom)
    .append("g")
    .attr("transform",
      "translate(" + margin.left + "," + margin.top + ")");

  // set the sankey diagram properties
  const sankeyPlot = d3_sankey.sankey<Node, Link>()
    .nodeWidth(36)
    .nodePadding(40)
    .size([width, height]);

  const path = sankeyPlot.links();

  // set up nodes for graph
  const nodes: Node[] = []
  data.forEach((row, index, rowData) => {
    // only push if distinct
    if (!(row.source in nodes)) {
      nodes.push(<Node>{ name: row.source, index: index });
    }
    if (!(row.target in nodes)) {
      nodes.push(<Node>{ name: row.target, index: index });
    }
  });

  const graph = { nodes: nodes, links: data };

  console.log(data);

  // replace node name with index
  graph.links.forEach((link, index, linksList) => {
    graph.links[index].source = graph.nodes.findIndex((value) => value.name == graph.links[index].source);
    graph.links[index].target = graph.nodes.findIndex((value) => value.name == graph.links[index].target);
  })

  console.log(graph.links);

  const plot = sankeyPlot(graph);

  // add sankey elements to the svg canvas
  const link = svg.append("g").selectAll(".link")
    .data(plot.links)
    .enter().append("path")
    .attr("class", "link")
    .attr("d", d3_sankey.sankeyLinkHorizontal())
    .attr("stroke-width", d => (typeof d.width === "number") ? d.width : null);

  // add the link titles
  link.append("title")
    .text(function(d) {
      return d.source + " → " +
        d.target + "\n" + format(d.value); });

  const node = svg.append("g").selectAll(".node")
    .data(plot.nodes)
    .enter().append("g")
    .attr("class", "node");


  // add the rectangles for the nodes
  node.append("rect")
    .attr("x", d => (typeof d.x0 === "number") ? d.x0 : null)
    .attr("y", d => (typeof d.y0 === "number") ? d.y0 : null)
    .attr("height", d => (typeof d.y0 === "number" && typeof d.y1 === "number") ? d.y1 - d.y0 : null)
    .attr("width", sankeyPlot.nodeWidth())
    .style("fill", d => color(d.name.replace(/ .*/, ""))) //omitted stroke
    .append("title")
    .text(d => d.name); //omitted node value

  // add title for the nodes
  node.append("text")
    .attr("x", d => (typeof d.x0 === "number") ? d.x0 - 6: null)
    .attr("y", d => (typeof d.y0 === "number" && typeof d.y1 === "number") ? (d.y1 + d.y0)/2 : null)
    .attr("dy", "0.35em")
    .attr("text-anchor", "end")
    .text(d => d.name)
    .filter(d => (typeof d.x0 === "number") ? d.x0 < width / 2: true)
    .attr("x", d => (typeof d.x1 === "number") ? d.x1 + 6: null)
    .attr("text-anchor", "start");
}
