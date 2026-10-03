'use strict';
importScripts('./solver.js');
let s,running=false,queued=false,target=0,history=[];
function snapshot(){const stats=s.coreStats(),i=stats[3],row=[s.time,...stats.slice(0,3),s.x(i%s.nx),s.y(Math.floor(i/s.nx)%s.ny),s.z(Math.floor(i/(s.nx*s.ny))),s.flipsAt(s.time),s.panPower,s.airPower,s.energy(),s.inputEnergy,s.boxEnergy,s.energyBalanceError()];
 if(!history.length||history[history.length-1][0]!==s.time)history.push(row);if(history.length>2000)history=history.filter((_,i)=>i===0||i%2===0||i===history.length-1);
 return {time:s.time,stats,field:s.temperatures(),peak:s.peakTemperature,surface:s.surfaceTemperatures(),flux:s.inwardFluxes(),row,steps:s.steps,constraintResidual:s.boundaryResidual,maxConstraintResidual:s.maxBoundaryResidual,constraintIterations:s.constraintIterations,auxiliaryPower:s.auxPower,nextFlip:s.nextFlip(),running,history:history.map(r=>r.slice())};}
function send(){postMessage({type:'frame',...snapshot()});}
function queue(){if(!queued&&running){queued=true;setTimeout(tick,0);}}
function tick(){queued=false;if(!running||!s)return;try{const reached=s.advanceTo(target,1);if(reached){if(s.time>=s.c.end)running=false;send();target=Math.min(s.c.end,s.time+s.c.end/100);}queue();}catch(e){running=false;postMessage({type:'error',message:e.message});}}
onmessage=({data:m})=>{try{
 if(m.type==='init'){running=false;s=new HeatLab.Solver(m.config);history=[];postMessage({type:'ready',grid:{nx:s.nx,ny:s.ny,nz:s.nz,h:s.h,x0:s.x0,y0:s.y0,z0:s.z0,inside:s.inside,core:s.core,coreIndices:s.coreIndices,geometry:s.geometry,contact:s.contact,stableDt:s.stableDt,maskResidual:s.maskResidual},...snapshot()});}
 else if(m.type==='run'&&s){running=s.time<s.c.end;target=Math.min(s.c.end,s.time+s.c.end/100);send();queue();}
 else if(m.type==='pause'&&s){running=false;send();}
}catch(e){running=false;postMessage({type:'error',message:e.message});}};
