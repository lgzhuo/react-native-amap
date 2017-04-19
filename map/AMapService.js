/**
 * Created by lgzhuo on 2017/3/16.
 */
import {NativeModules, Alert} from 'react-native'
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

class AMapService {
    static async calculateNaviDriveRoute(props: NaviDriveProps): Promise<NaviRoute[]> {
        return await AMS.calculateNaviDriveRoute(props)
    }

    static async getCurrentPosition(props: LocationProps): Promise<GeoLocation> {
        return await AMS.getCurrentPosition(props)
    }
}

export default AMapService;