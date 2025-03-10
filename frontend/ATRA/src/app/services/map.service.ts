import { Injectable } from '@angular/core';
import L, { map } from 'leaflet';

@Injectable({
  providedIn: 'root'
})
export class MapService {
  static mapSetup(mapContainerId:string): L.Map{
    const baseMaps = {
      "OSM Standard": L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png'),
      "Satellite": L.tileLayer('https://tiles.stadiamaps.com/tiles/alidade_satellite/{z}/{x}/{y}{r}.jpg'),
      "Vector": L.tileLayer('https://tiles.stadiamaps.com/tiles/outdoors/{z}/{x}/{y}{r}.jpg')
    };

    const map = L.map(mapContainerId, {
      layers: [baseMaps["OSM Standard"]]
    })
    // Add control to switch between layers
    L.control.layers(baseMaps).addTo(map);
    return map
  }

  static addPolyline(coordinates:[lat:number, lon:number][], map:L.Map, prevPolyline?:L.Polyline) {
    if (prevPolyline!=null) {prevPolyline.remove()}
    const polyline = L.polyline(coordinates).addTo(map)
    map.fitBounds(polyline.getBounds());
    //{
    //  color: 'blue',         // Line color
    //  weight: 4,             // Line thickness
    //  opacity: 0.8,          // Line opacity
    //}
    return polyline
  }
}
