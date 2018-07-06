# voroni-postals

[![Build Status](https://travis-ci.org/danwatt/voroni-postals.svg?branch=master)](https://travis-ci.org/danwatt/voroni-postals)

* [Blog post description](https://www.danwatt.org/2016/06/postal-code-voroni-diagram/)
* [Heroku](http://voroni-postals.herokuapp.com/postals.html) - This app is deployed to a free Heroku instance, so it may shut down due to inactivity. 

## Generating a simplified US Border

``` bash
brew install npm gdal
npm install -g mapshaper
mapshaper ne_50m_admin_1_states_provinces_lakes.shp -filter 'sov_a3=="US1"' -dissolve2 -o us1.json
mapshaper ne_50m_admin_1_states_provinces_lakes.shp -filter 'sov_a3=="CAN"' -dissolve2 -o can.json
mapshaper ne_50m_admin_0_countries_lakes.shp -filter 'ADM0_A3=="MEX"' -dissolve2 -o mex.json

ogr2ogr -f CSV /vsistdout/ us1.json -lco GEOMETRY=AS_WKT    | tail -n +2 | sed 's/)"//g' | sed 's/"GEOMETRYCOLLECTION (//g' > us1.wkt
ogr2ogr -f CSV /vsistdout/ can.json -lco GEOMETRY=AS_WKT    | tail -n +2 | sed 's/)"//g' | sed 's/"GEOMETRYCOLLECTION (//g' > can.wkt
ogr2ogr -f CSV /vsistdout/ mex.json -lco GEOMETRY=AS_WKT    | tail -n +2 | sed 's/)"//g' | sed 's/"GEOMETRYCOLLECTION (//g' > mex.wkt
```