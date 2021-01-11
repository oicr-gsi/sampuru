/// <reference types="d3" />
/// <reference types="d3-sankey" />

import * as d3 from 'd3';
import * as d3Sankey from 'd3-sankey';

import { SankeyTransition } from "./data-transfer-objects.js";

export interface SankeyData {
  source: string | number,
  target: string | number,
  value: number,
  color: string
}

export interface Node {
  name: string,
  index: number
}

/**
 * Return formatted source and target node names, and value
 * */
export function linkSetup(key: keyof SankeyTransition, sankey: SankeyTransition): SankeyData {
  switch(key) {
    case "receipt":
      return { source: "Receipt", target: "Extraction", value: sankey[key].extraction, color: "#437bbf" };
    case "extraction":
      return { source: "Extraction", target: "Library Preparation", value: sankey[key].library_preparation, color: "#437bbf" };
    case "library_preparation":
      return { source: "Library Preparation", target: "Low-Pass Sequencing", value: sankey[key].low_pass_sequencing, color: "#437bbf" };
    case "low_pass_sequencing":
      return { source: "Low-Pass Sequencing", target: "Full-Depth Sequencing", value: sankey[key].full_depth_sequencing, color: "#437bbf" };
    case "full_depth_sequencing":
      return { source: "Full-Depth Sequencing", target: "Informatics Interpretation", value: sankey[key].informatics_interpretation, color: "#437bbf" };
    case "informatics_interpretation":
      return { source: "Informatics Interpretation", target: "Final Report", value: sankey[key].final_report, color: "#437bbf" };
    case "final_report":
      return { source: "Final Report", target: "Passed", value: sankey[key].passed, color: "#437bbf" };
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
            value: link.value,
            color: link.color
          });
      }

      if (sankey[key].pending > 0) {
        data.push(
          {
            source: link.source,
            target: "Pending",
            value: sankey[key].pending,
            color: "#fcc874"
          });
      }

      if (sankey[key].failed > 0) {
        data.push(
          {
            source: link.source,
            target: "Failed",
            value: sankey[key].failed,
            color: "#c42d2d"
          });
      }
    });

  return data;
}

export function distinctBy<T>(key: keyof T, array: T[]) {
  const keys = array.map(value => value[key]);
  return array.filter((value, index) => keys.indexOf(value[key]) === index);
}

export function colorNodes(nodeName: string): string {
  if(nodeName == "Pending") {
    return "#fcc874"
  } else if(nodeName == "Failed") {
    return "#c42d2d"
  } else {
    return "#437bbf"
  }
}

export function drawSankey(sankey: SankeyTransition) {
  const data = preprocess(sankey);

  // set the dimensions and margins of the graph
  const margin = {top: 10, right: 10, bottom: 10, left: 10},
    width = 900 - margin.left - margin.right,
    height = 300 - margin.top - margin.bottom;

  // format variables
  const formatNumber = d3.format(",.0f"),    // zero decimal places
    format = function (d: number | undefined) {
      return (typeof d === "number" ? formatNumber(d): null);
    };

  // set up nodes for graph
  const nodes: Node[] = []
  data.forEach((row, index, rowData) => {
    nodes.push(<Node>{ name: row.source, index: index });
    nodes.push(<Node>{ name: row.target, index: index });
  });

  const graph = { nodes: nodes, links: data };

  // only want distinct nodes
  graph.nodes = distinctBy('name', graph.nodes);

  // replace node name with index
  graph.links.forEach((link, index, linksList) => {
    graph.links[index].source = graph.nodes.findIndex((value) => value.name == graph.links[index].source);
    graph.links[index].target = graph.nodes.findIndex((value) => value.name == graph.links[index].target);
  });

  // set the sankey diagram properties
  const sankeyPlot = d3Sankey.sankey<Node, SankeyData>()
    .nodeWidth(20)
    .nodePadding(25)
    .size([width, height]);

  // instantiate view box and append to sankey div
  const svg = d3.select("div#sankey")
    .append("svg")
    .attr("viewBox", `0 0 ${width} ${height}`);

  // pass nodes and links to sankey diagram that was created above
  const plot = sankeyPlot(graph);

  // draw node rectangles
  svg.append("g")
    .attr("stroke", "#000")
    .selectAll("rect")
    .data(plot.nodes)
    .join("rect")
    .attr("x", d => (typeof d.x0 === "number") ? d.x0 : null)
    .attr("y", d => (typeof d.y0 === "number") ? d.y0 : null)
    .attr("height", d => (typeof d.y0 === "number" && typeof d.y1 === "number") ? d.y1 - d.y0 : null)
    .attr("width", d => (typeof d.x0 === "number" && typeof d.x1 === "number") ? d.x1 - d.x0 : null)
    .attr("fill", d => colorNodes(d.name))
    .append("title")
    .text(d => `${d.name}\n${format(d.value)}`);

  // draw links
  const link = svg.append("g")
    .attr("fill", "none")
    .attr("stroke-opacity", 0.3)
    .selectAll("g")
    .data(plot.links)
    .join("g")
    .style("mix-blend-mode", "multiply");

  // format position and color of links
  link.append("path")
    .attr("d", d3Sankey.sankeyLinkHorizontal())
    .attr("stroke", d => d.color)
    .attr("stroke-width", d => (typeof d.width === "number") ? Math.max(1, d.width) : Math.max(1, 50));

  // add titles to link
  link.append("title")
    .text(d => (typeof d.target === "object" && typeof d.source === "object") ?
      `${d.source.name} → ${d.target.name}\n${format(d.value)}` : "")

  // add title for nodes
  svg.append("g")
    .attr("font-family", "sans-serif")
    .attr("font-size", 8)
    .selectAll("text")
    .data(plot.nodes)
    .join("text")
    .attr("x", d => (typeof d.x0 === "number" && typeof d.x1 === "number") ? d.x0 < width / 2 ? d.x1 + 6 : d.x0 - 6: null)
    .attr("y", d => (typeof d.y0 === "number" && typeof d.y1 === "number") ? (d.y1 + d.y0)/2 : null)
    .attr("dy", "0.35em")
    .attr("text-anchor", d => (typeof d.x0 === "number") ? (d.x0 < width / 2 ? "start" : "end") : null)
    .text(d => d.name);
}