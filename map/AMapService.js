/**
 * Created by lgzhuo on 2017/3/16.
 */
import {NativeModules} from 'react-native'

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
export type StartNaviProps = {}

// http://lbs.amap.com/api/amap-mobile/guide/ios/route
// http://lbs.amap.com/api/amap-mobile/guide/android/route
export type AMapRouteProps = {
    sourceApplication: string,//第三方调用应用名称
    dlat: number,             //终点纬度
    dlon: number,             //终点经度
    dev: 0 | 1,               //起终点是否偏移(0:lat 和 lon 是已经加密后的,不需要国测加密; 1:需要国测加密)
    t: 0 | 1 | 2 | 3 | 4 | 5, //t = 0（驾车）= 1（公交）= 2（步行）= 3（骑行）= 4（火车）= 5（长途客车）
    sid?: string,             //起点的POIID
    slat?: number,            //起点纬度。如果不填写此参数则自动将用户当前位置设为起点纬度。
    slon?: number,            //起点经度。 如果不填写此参数则自动将用户当前位置设为起点经度。
    sname?: string,           //起点名称
    did?: string,             //终点的POIID
    dname?: string            //终点名称
}

/**
 * http://lbsyun.baidu.com/index.php?title=uri/api/android
 * http://lbsyun.baidu.com/index.php?title=uri/api/ios
 *
 * origin和destination二者至少一个有值（默认值是当前定位地址）
 *
 */
export type baiduMapRouteProps = {
    origin?: string,                                    //起点名称或经纬度，或者可同时提供名称和经纬度，此时经纬度优先级高，将作为导航依据，名称只负责展示
    destination?: string,                               //终点名称或经纬度，或者可同时提供名称和经纬度，此时经纬度优先级高，将作为导航依据，名称只负责展示。
    mode?: 'transit' | 'driving' | 'walking' | 'riding',//导航模式，可选transit（公交）、driving（驾车）、walking（步行）和riding（骑行）.默认:driving
    region?: string,                                    //城市名或县名
    origin_region?: string,                             //起点所在城市或县
    destination_region?: string,                        //终点所在城市或县
    sy?: 0 | 2 | 3 | 4 | 5 | 6,                         //@platform android. 公交检索策略，只针对mode字段填写transit情况下有效，值为数字。 0：推荐路线 2：少换乘 3：少步行 4：不坐地铁 5：时间短 6：地铁优先
    index?: number,                                     //@platform android. 公交结果结果项，只针对公交检索，值为数字，从0开始
    target?: 0 | 1,                                     //@platform android. 0 图区，1 详情，只针对公交检索有效
    coord_type?:string,                                 //@platform web,ios. 坐标类型，可选参数，默认为bd09ll。
    zoom?:number,                                       //@platform web,ios. 展现地图的级别，默认为视觉最优级别。
    src?:string                                         //@platform web,ios. 调用来源，ios 规则：webapp.navi.yourCompanyName.yourAppName
}

class AMapService {
    static async calculateNaviDriveRoute(props: NaviDriveProps): Promise<NaviRoute[]> {
        return await AMS.calculateNaviDriveRoute(props)
    }

    static async getCurrentPosition(props: LocationProps): Promise<GeoLocation> {
        return await AMS.getCurrentPosition(props)
    }

    static async poiSearch(props): Promise<POISearchResponse> {
        return await AMS.poiSearch({pageSize: 10, pageNum: 0, cityLimit: true, ...props})
    }

    static startNavi(props: StartNaviProps) {
        AMS.startNavi(props)
    }

    static callAMapRoute(props: AMapRouteProps) {
        AMS.callAMapRoute(props)
    }

    static callBaiduMapRoute(props: baiduMapRouteProps) {
        AMS.callBaiduMapRoute(props)
    }
}

export default AMapService;