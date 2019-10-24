/**
 * Created by lgzhuo on 2017/3/16.
 */
import {NativeModules, Platform, Linking} from 'react-native'
import URI from 'urijs'

const AMS = NativeModules.AMapService;

type LatLng = {
    latitude: number,
    longitude: number
}

type Bounds = {
    southwest: LatLng,
    northeast: LatLng
}

export type NaviDriveProps = {
    from?: LatLng,          //起始点
    to: LatLng,             //目标点
    wayPoints?: LatLng[],   //途经点

    multipleRoute?: boolean,    //是否多路径规划
    avoidCongestion?: boolean,  //是否躲避拥堵
    avoidHighway?: boolean,     //是否不走高速
    avoidCost?: boolean,        //是否避免收费
    prioritiseHighway?: boolean //是否高速优先
}

export type NaviLink = {
    coordinates: LatLng[],
    length: number,
    time: number,
    roadName: string,
    roadClass: number,
    roadType: number,
    hasTrafficLights: boolean
}

export type NaviStep = {
    coordinates: LatLng[],//坐标点集
    length: number,//长度
    time: number,//耗时
    chargeLength: number,//收费路段长度
    trafficLightNumber?: number, //红绿灯总数
    links: NaviLink[]
}

export type NaviRoute = {
    id: number,
    length: number,//导航路径总长度(单位:米)
    time: number,//导航路径所需的时间(单位:秒)
    bounds: Bounds,//导航路线最小坐标点和最大坐标点围成的矩形区域,
    center: LatLng,//导航路线的中心点，即导航路径的最小外接矩形对角线的交点
    start: LatLng,
    end: LatLng,
    coordinates: LatLng[],
    strategy: number,
    tollCost: number,//花费金额，单位元。

    steps: NaviStep[],
    stepsCount: number,
    wayPoints: LatLng[],//途经点坐标
    wayPointIndexes: number[]

}

export type LocationProps = {}

export type GeoLocation = {
    latitude: number,
    longitude: number,
    accuracy: number,
    adCode: string,
    address: string,
    altitude: number,
    aoiName: string,
    bearing: number,
    buildingId: string,
    city: string,
    cityCode: string,
    country: string,
    district: string,
    floor: number,
    poiName: string,
    province: string,
    speed: number,
    street: string,
    streetNum: string
}

export type POISearchProps = {
    keyWord: string,
    city: string,
    pageSize: number,
    pageNum: number,
    cityLimit: boolean
}

export type POISearchResponse = {
    adCode?: string,
    adName?: string,
    businessAres?: string,
    cityCode?: string,
    cityName?: string,
    direction?: string,
    distance?: number,
    email?: string,
    enter?: LatLng,
    exit?: LatLng,
    location: LatLng,
    title: string
}
export type StartNaviProps = {
    id?: number,
    type?: 'walk' | 'drive'
}

type Coordinate = 'wgs84' | 'gcj02' | 'bd09'

export type MapRouteProps = {
    src: string,//调用者APP的名称或包名
    sLatLng?: LatLng,//起点经纬度
    sName?: string,//起点名称
    sRegion?: string,//起点所在县市
    dLatLng?: LatLng,//终点经纬度
    dName?: string,//终点名称
    dRegion?: string,//终点所在县市
    mode?: 'car' | 'bus' | 'walk' | 'ride',//路径规划类型，默认驾车导航
    coordinate?: Coordinate//传入的坐标体系
}

// http://lbs.amap.com/api/amap-mobile/guide/android/route
// http://lbs.amap.com/api/amap-mobile/guide/ios/route
const amapRouteUriCreator = (baseUri: string) => (props: MapRouteProps): string => {
    let dev, t;
    switch (props.mode) {
        case 'bus':
            t = 1;
            break;
        case 'walk':
            t = 2;
            break;
        case 'ride':
            t = 3;
            break;
        case 'car':
        default:
            t = 0;
            break;

    }
    switch (props.coordinate) {
        case 'wgs84':
            dev = 1;
            break;
        case 'bd09':
            dev = 0;//TODO 转坐标
            break;
        case 'gcj02':
        default:
            dev = 0;
            break;
    }
    let uri = URI(baseUri)
        .addSearch('sourceApplication', props.src)
        .addSearch('dev', dev)
        .addSearch('t', t)
        .addSearch('sname', props.sName)
        .addSearch('dname', props.dName);
    if (props.sLatLng) {
        uri.addSearch('slat', props.slat)
            .addSearch('slon', props.slon)
    }
    if (props.dLatLng) {
        uri.addSearch('dlat', props.dLatLng.latitude)
            .addSearch('dlon', props.dLatLng.longitude)
    }
    return uri.toString()
};

const amapRouteUriAndroid = amapRouteUriCreator('amapuri://route/plan');

const amapRouteUriIOS = amapRouteUriCreator('iosamap://path');

//http://lbs.amap.com/api/uri-api/guide/travel/route
function amapRouteUriWeb(props: MapRouteProps): string {
    let coordinate, from, to;
    switch (props.coordinate) {
        case 'wgs84':
            coordinate = 'wgs84';
            break;
        case 'bd09':
            coordinate = 'gaode';//TODO 转坐标
            break;
        case 'gcj02':
        default:
            coordinate = 'gaode';
            break;
    }
    if (props.sLatLng) {
        from = `${props.sLatLng.longitude},${props.sLatLng.latitude}`;
        if (props.sName) {
            from = `${from},${props.sName}`
        }
    }
    if (props.dLatLng) {
        to = `${props.dLatLng.longitude},${props.dLatLng.latitude}`;
        if (props.dName) {
            to = `${to},${props.dName}`
        }
    }
    let uri = URI('http://uri.amap.com/navigation')
        .addSearch('src', props.src)
        .addSearch('mode', props.mode)
        .addSearch('coordinate', coordinate)
        .addSearch('callnative', 1)
        .addSearch('from', from)
        .addSearch('to', to);
    return uri.toString()
}

async function amapRoute(props: MapRouteProps) {
    let amapRouteUriCreator, amapUri, uri;
    switch (Platform.OS) {
        case 'ios':
            amapRouteUriCreator = amapRouteUriIOS;
            amapUri = 'iosamap://path';
            break;
        case 'android':
            amapRouteUriCreator = amapRouteUriAndroid;
            amapUri = 'amapuri://route/plan';
            break;
        default:
            amapRouteUriCreator = amapRouteUriWeb;
            amapUri = 'http://uri.amap.com/navigation';
            break;
    }
    const support = await Linking.canOpenURL(amapUri);
    if (support) {
        uri = amapRouteUriCreator(props);
    } else {
        uri = amapRouteUriWeb(props)
    }
    await Linking.openURL(uri);
    return uri
}

function baidumapPoint(latLng?: LatLng, name?: string): string | void {
    let point;
    if (latLng) {
        point = `${latLng.latitude},${latLng.longitude}`;
        if (name) {
            point = `latlng:${point}|name:${name}`
        }
    }
    return point
}

//http://lbsyun.baidu.com/index.php?title=uri/api/android
//http://lbsyun.baidu.com/index.php?title=uri/api/ios
function baidumapRouteUri(props: MapRouteProps): string {
    let origin = baidumapPoint(props.sLatLng, props.sName),
        destination = baidumapPoint(props.dLatLng, props.dName),
        mode;
    switch (props.mode) {
        case 'bus':
            mode = 'transit';
            break;
        case 'walk':
            mode = 'walking';
            break;
        case 'ride':
            mode = 'riding';
            break;
        case 'car':
        default:
            mode = 'driving';
            break;
    }
    let uri = URI('baidumap://map/direction')
        .addSearch('origin', origin)
        .addSearch('destination', destination)
        .addSearch('mode', mode)
        .addSearch('coord_type', props.coordinate)
        .addSearch('src', props.src);
    return uri.toString();
}

//http://lbsyun.baidu.com/index.php?title=uri/api/web
function baidumapRouteUriWeb(props: MapRouteProps): string {
    let origin = baidumapPoint(props.sLatLng, props.sName),
        destination = baidumapPoint(props.dLatLng, props.dName),
        mode;
    switch (props.mode) {
        case 'bus':
            mode = 'transit';
            break;
        case 'walk':
        case 'ride':
            mode = 'walking';
            break;
        case 'car':
        default:
            mode = 'driving';
            break;
    }
    let uri = URI('http://api.map.baidu.com/direction')
        .addSearch('origin', origin)
        .addSearch('destination', destination)
        .addSearch('mode', mode)
        .addSearch('output', 'html')
        .addSearch('coord_type', props.coordinate)
        .addSearch('src', props.src)
        //baidu web uri 必须指定region
        .addSearch('origin_region', props.sRegion || 'b')
        .addSearch('destination_region', props.dRegion || 'b');
    return uri.toString();
}

async function baidumapRoute(props: MapRouteProps) {
    const support = await Linking.canOpenURL('baidumap://map/direction');
    let uri;
    if (support) {
        uri = baidumapRouteUri(props)
    } else {
        uri = baidumapRouteUriWeb(props)
    }
    await Linking.openURL(uri);
    return uri
}

export type MapMarkProps = {
    src: string,
    latLng: LatLng,
    name?: string,
    content?: string,
    coordinate?: Coordinate
}

const amapMarkUriCreator = (baseUri: string) => (props: MapMarkProps): string => {
    let dev;
    switch (props.coordinate) {
        case 'wgs84':
            dev = 1;
            break;
        case 'bd09':
            dev = 0;//TODO 转坐标
            break;
        case 'gcj02':
        default:
            dev = 0;
            break;
    }
    let uri = URI(baseUri)
        .addSearch('sourceApplication', props.src)
        .addSearch('poiname', props.name)
        .addSearch('lat', props.latLng.latitude)
        .addSearch('lon', props.latLng.longitude)
        .addSearch('dev', dev);
    return uri.toString()
};

const amapMarkUriAndroid = amapMarkUriCreator('androidamap://viewMap');

const amapMarkUriIOS = amapMarkUriCreator('iosamap://viewMap');

function amapMarkUriWeb(props: MapMarkProps) {
    let coordinate;
    switch (props.coordinate) {
        case 'wgs84':
            coordinate = 'wgs84';
            break;
        case 'bd09':
            coordinate = 'gaode';//TODO 转坐标
            break;
        case 'gcj02':
        default:
            coordinate = 'gaode';
            break;
    }
    let uri = URI('http://uri.amap.com/marker')
        .addSearch('src', props.src)
        .addSearch('position', `${props.latLng.longitude},${props.latLng.latitude}`)
        .addSearch('name', props.name);
    return uri.toString()
}

async function amapMark(props: MapMarkProps) {
    let amapMarkUri, markUri, uri;
    switch (Platform.OS) {
        case 'ios':
            amapMarkUri = amapMarkUriIOS;
            markUri = 'iosamap://viewMap';
            break;
        case 'android':
            amapMarkUri = amapMarkUriAndroid;
            markUri = 'androidamap://viewMap';
            break;
        default:
            amapMarkUri = amapMarkUriWeb;
            markUri = 'http://uri.amap.com/marker';
            break;
    }
    const support = await Linking.canOpenURL(markUri);
    if (support) {
        uri = amapMarkUri(props);
    } else {
        uri = amapMarkUriWeb(props)
    }
    await Linking.openURL(uri);
    return uri
}

function baidumapMarkUri(props: MapMarkProps): string {
    let uri = URI('baidumap://map/marker')
        .addSearch('location', `${props.latLng.latitude},${props.latLng.longitude}`)
        .addSearch('title', props.name)
        .addSearch('content', props.content)
        .addSearch('coord_type', props.coordinate)
        .addSearch('src', props.src);
    return uri.toString()
}

function baidumapMarkUriWeb(props: MapMarkProps): string {
    let uri = URI('http://api.map.baidu.com/marker')
        .addSearch('location', `${props.latLng.latitude},${props.latLng.longitude}`)
        .addSearch('title', props.name)
        .addSearch('content', props.content)
        .addSearch('coord_type', props.coordinate)
        .addSearch('src', props.src)
        .addSearch('output', 'html');
    return uri.toString()
}

async function baidumapMark(props: MapMarkProps) {
    const support = await Linking.canOpenURL('baidumap://map/marker');
    let uri;
    if (support) {
        uri = baidumapMarkUri(props)
    } else {
        uri = baidumapMarkUriWeb(props)
    }
    await Linking.openURL(uri);
    return uri
}

export type POIProps = {
    keywords: string | string[],
    location?: LatLng | LatLng[],
    coordinate?: Coordinate,
    src: string,
    region?: string,
}

function amapPOIUriCreator(baseUri: string): POIProps => string {
    return function (props: POIProps) {
        let keywords = props.keywords, dev;
        if (Array.isArray(props.keywords)) {
            keywords = props.keywords.join('|')
        }
        switch (props.coordinate) {
            case 'wgs84':
                dev = 1;
                break;
            case 'bd09':
                dev = 0;//TODO 转坐标
                break;
            case 'gcj02':
            default:
                dev = 0;
                break;
        }
        let uri = URI(baseUri)
            .addSearch('keywords', keywords)
            .addSearch('sourceApplication', props.src)
            .addSearch('dev', dev);
        if (Array.isArray(props.location)) {
            props.location.map((loc, idx) => {
                uri.addSearch(`lat${idx + 1}`, loc.latitude);
                uri.addSearch(`lon${idx + 1}`, loc.longitude)
            })
        } else if (typeof props.location === 'object') {
            uri.addSearch('lat1', props.location.latitude)
                .addSearch('lon1', props.location.longitude)
        }
        return uri.toString()
    }
}

const amapPOIUriAndroid = amapPOIUriCreator('androidamap://poi');

const amapPOIUriIOS = amapPOIUriCreator('iosamap://poi');

function amapPOIUriWeb(props: POIProps) {
    let keyword = props.keywords,
        center = Array.isArray(props.location) ? props.location[0] : props.location,
        coordinate;
    if (Array.isArray(props.keywords)) {
        keyword = props.keywords[0]
    }
    if (center) {
        center = `${center.longitude},${center.latitude}`
    }
    switch (props.coordinate) {
        case 'wgs84':
            coordinate = 'wgs84';
            break;
        case 'bd09':
            coordinate = 'gaode';//TODO 转坐标
            break;
        case 'gcj02':
        default:
            coordinate = 'gaode';
            break;
    }
    let uri = URI('http://uri.amap.com/search')
        .addSearch('keyword', keyword)
        .addSearch('center', center)
        .addSearch('city', props.region)
        .addSearch('view', 'map')
        .addSearch('src', props.src)
        .addSearch('coordinate', coordinate);
    return uri.toString()
}

async function amapPOI(props: POIProps) {
    let amapPOIUri, poiUri, uri;
    switch (Platform.OS) {
        case 'ios':
            amapPOIUri = amapPOIUriIOS;
            poiUri = 'iosamap://poi';
            break;
        case 'android':
            amapPOIUri = amapPOIUriAndroid;
            poiUri = 'androidamap://poi';
            break;
        default:
            amapPOIUri = amapPOIUriWeb;
            poiUri = 'http://uri.amap.com/search';
            break;
    }
    const support = await Linking.canOpenURL(poiUri);
    if (support) {
        uri = amapPOIUri(props);
    } else {
        uri = amapPOIUriWeb(props)
    }
    await Linking.openURL(uri);
    return uri
}

function baidumapPOIUri(props: POIProps) {
    let query = Array.isArray(props.keywords) ? props.keywords[0] : props.keywords,
        location = Array.isArray(props.location) ? props.location[0] : props.location;
    if (location) {
        location = `${location.latitude},${location.longitude}`
    }
    let uri = URI('baidumap://map/place/search')
        .addSearch('query', query)
        .addSearch('location', location)
        .addSearch('coord_type', props.coordinate)
        .addSearch('src', props.src)
        .addSearch('region', props.region);
    return uri.toString()
}

function baidumapPOIUriWeb(props: POIProps) {
    let query = Array.isArray(props.keywords) ? props.keywords[0] : props.keywords,
        location = Array.isArray(props.location) ? props.location[0] : props.location;
    if (location) {
        location = `${location.latitude},${location.longitude}`
    }
    let uri = URI('http://api.map.baidu.com/place/search')
        .addSearch('query', query)
        .addSearch('location', location)
        .addSearch('output', 'html')
        .addSearch('coord_type', props.coordinate)
        .addSearch('src', props.src)
        .addSearch('region', props.region || 'b');
    return uri.toString()
}

async function baidumapPOI(props: POIProps) {
    const support = await Linking.canOpenURL('baidumap://map/place/search');
    let uri;
    if (support) {
        uri = baidumapPOIUri(props)
    } else {
        uri = baidumapPOIUriWeb(props)
    }
    await Linking.openURL(uri);
    return uri
}

let type;

async function calculateNaviDriveRoute(props: NaviDriveProps): Promise<NaviRoute[]> {
    const routes = await AMS.calculateNaviDriveRoute(props);
    type = 'drive';
    return routes
}

async function calculateNaviWalkRoute(props): Promise<NaviRoute[]> {
    const routes = await AMS.calculateNaviWalkRoute(props);
    type = 'walk';
    return routes
}

async function poiSearch(props: POISearchProps): Promise<POISearchResponse> {
    return await AMS.poiSearch({pageSize: 10, pageNum: 0, cityLimit: true, ...props})
}

function poiAroundSearch(props: {
  keyWord: string,
  types: string,
  city: string,
  location: {
    latitude: number,
    longitude: number
  },
  radius: number,
  pageSize: number,
  pageNum: number, // from 1
}): Promise<{
  adCode: string,
  adName: string,
  businessAres: string,
  cityCode: string,
  cityName: string,
  direction: string,
  distance: number,
  email: string,
  enter: {
    latitude: number,
    longitude: number
  },
  exit: {
    latitude: number,
    longitude: number
  },
  location: {
    latitude: number,
    longitude: number
  },
  title: string,
  address: string
}> {
  return AMS.poiAroundSearch({ pageSize: 10, pageNum: 0, ...props });
}

function startNavi(props: StartNaviProps) {
    AMS.startNavi({type, ...props})
}

export default {
    calculateNaviDriveRoute,
    calculateNaviWalkRoute,
    poiSearch,
    poiAroundSearch,
    startNavi,
    amapRoute,
    amapMark,
    amapPOI,
    baidumapRoute,
    baidumapMark,
    baidumapPOI
}

