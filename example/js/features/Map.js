/**
 * Created by yunrui on 2017/3/7.
 */
import React, {Component} from 'react';
import {
    StyleSheet,
    Text,
    View,
    TouchableOpacity,
    Alert
} from 'react-native';
import AMap,{AMapMarker,AMapService,AMapPolyline,AMapCallout} from 'react-native-amap'
import CustomCallout from '../CustomCallout'

let clusterData = [];
for (let i=0;i<100;i++){
    clusterData.push({
        latitude:30.2763386988 + Math.random()*0.01,
        longitude:120.1232028758 + Math.random()*0.01
    })
}

export class Map extends Component {

    static FeatureName = '地图';

    state = {
        clustered:[],
        size:20
    };

    render() {
        return (
            <View style={styles.container}>
                <AMap showsUserLocation={true}
                      clusterData={clusterData}
                      clusterSize={30}
                      ref="map"
                onCluster={this._onCluster}>
                    {/*{this.state.clustered.map(({coordinate,points},idx)=>{*/}
                        {/*return (*/}
                            {/*<AMapMarker coordinate={coordinate} key={idx}>*/}
                                {/*<View style={styles.cluster}>*/}
                                    {/*<Text style={styles.clusterText}>*/}
                                        {/*{points.length}*/}
                                    {/*</Text>*/}
                                {/*</View>*/}
                            {/*</AMapMarker>*/}
                        {/*)*/}
                    {/*})}*/}

                    {/*<AMapMarker coordinate={{latitude: 30.2763386988, longitude: 120.1232028758}}*/}
                                {/*title="Title"*/}
                                {/*description="Snippet"*/}
                                {/*onSelectChange={selected => Alert.alert(`selected:${selected}`)}*/}
                                {/*calloutEnabled={true}*/}
                                {/*onCalloutPress={() => Alert.alert('call out press')}>*/}

                    {/*</AMapMarker>*/}

                    <AMapMarker coordinate={{latitude: 30.2773386988, longitude: 120.1242028758}}
                                title="fhe"
                                calloutEnabled={true}>
                        <Text>
                            ioi   11
                        </Text>
                        <Text style={{color: 'red', fontSize: 11}}>
                            omn12
                        </Text>
                        <AMapCallout tooltip={true} style={{backgroundColor:'red'}}>
                            <Text>
                                tesssssssssssssiiiiieiiiejfienfiefiejfefmoemfoenfonofsnofns
                            </Text>

                        </AMapCallout>

                    </AMapMarker>

                    {/*<AMapPolyline coordinates={[*/}
                        {/*{latitude: 30.2773386988, longitude: 120.1242028758},*/}
                        {/*{latitude: 30.2783386988, longitude: 120.1252028758}*/}
                    {/*]}/>*/}
                </AMap>

                <TouchableOpacity onPress={this._focus} style={styles.focusContainer}>
                    <View style={styles.focus}>
                    </View>
                </TouchableOpacity>

                <TouchableOpacity onPress={()=>this.setState({size:this.state.size===20?30:20})} style={[styles.focusContainer,{top:100}]}>
                    <View style={styles.focus}>
                    </View>
                </TouchableOpacity>
            </View>
        );
    }

    _focus = () => {
        // this.refs.map.showAnnotations(10,true);
        this.refs.map.centerCoordinate({latitude: 30.2773386988, longitude: 120.1232028758});
        // AMapService.calculateNaviDriveRoute({
        //     from: {latitude: 30.2773386988, longitude: 120.1242028758},
        //     to: {latitude: 30.2883386988, longitude: 120.1352028758},
        //     multipleRoute:true
        // }).then(result => {
        //     console.log('caculate drive routes:', result);
        // }).catch(e => {
        //     console.log('caculate error:', e);
        // })
    };

    _onCluster = clustered => {
        this.setState({
            clustered
        })
    }
}

const styles = StyleSheet.create({
    container: {
        flex: 1,
        backgroundColor: '#F5FCFF',
    },
    welcome: {
        fontSize: 20,
        textAlign: 'center',
        margin: 10,
    },
    instructions: {
        textAlign: 'center',
        color: '#333333',
        marginBottom: 5,
    },
    focusContainer: {
        position: 'absolute',
        top: 25,
        left: 25,
    },
    focus: {
        width: 50,
        height: 50,
        backgroundColor: 'red',
    },
    cluster:{
        borderRadius:8,
        width:16,
        height:16,
        backgroundColor:'#3F8863',
        justifyContent:'center',
        alignItems:'center'
    },
    clusterText:{
        fontSize:14,
        color:'#333',
        backgroundColor:'transparent'
    }
});