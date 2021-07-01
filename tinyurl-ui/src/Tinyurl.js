import React, { Component } from 'react';
import pen from './pen.svg';
import logo from './logo.svg';
import top from './top.svg';
import bottom from './bottom.svg';
import axios from 'axios';
import qs from 'qs'
import './Tinyurl.css';
import { Input } from 'antd';
import { Select } from 'antd';
import { Button } from 'antd';
import { Card } from 'antd';

const { Option } = Select;

const { TextArea } = Input;

class Tinyurl extends Component {
    constructor(props) {
        super(props);
        this.state = {rawurl: '', baseurl: 't.vipgp88.com', code: '', tinyurl: ''}
        this.handleClick = this.handleClick.bind(this);
        this.rawurlChange = this.rawurlChange.bind(this);
        this.baseurlChange = this.baseurlChange.bind(this);
        this.codeChange = this.codeChange.bind(this);
    }

    rawurlChange(e) {
        const state = this.state;
        state.rawurl = e.target.value;
        this.setState(state);
    }

    baseurlChange(e) {
        const state = this.state;
        state.baseurl = e.target.value;
        this.setState(state);
    }

    codeChange(e) {
        const state = this.state;
        state.code = e.target.value;
        this.setState(state);
    }

    tinyurlChange(e) {
        const state = this.state;
        state.tinyurl = e.target.value;
        this.setState(state);
    }

    handleClick() {
        var api = "/service/tiny/url/create";
        console.log("code=" + this.state.code);
        console.log("stringify=" + JSON.stringify(this.state.code));
        if (this.state.code !== '' && JSON.stringify(this.state.code) !== "{}") {
            api = "/service/tiny/url/diy/create";
        }

        console.log("api=" + api);
        const state = this.state;
        var self = this;
        // Content-Type: application/x-www-form-urlencoded
        axios.post(api, qs.stringify({
            "longUrl": this.state.rawurl,
            "diyCode": this.state.code,
            "baseUrl": this.state.baseurl
        }))
            .then(function (response) {
                console.log("response=" + response.data.success);
                var result = response.data.result;
                if (response.data.success == 0) {
                    result = response.data.errorMessage;
                }
                console.log("result=" + result)
                state.tinyurl = result;
                self.setState(state);
            })
            .catch(function (error) {
                console.log(error);
            });

    }

    render() {
        return (
            <div>
                <div data-v-2d9bd78e className="main background-img">
                    <svg data-v-2d9bd78e="" width="510" height="227" viewBox="0 0 510 227" fill="none"
                         xmlns="http://www.w3.org/2000/svg" class="background-img-top">
                        <path fill-rule="evenodd" clip-rule="evenodd"
                              d="M-87.152 -39.8215C-88.9236 -97.5924 -78.1971 -154.94 -48.4234 -204.479C-20.7542 -250.517 24.1466 -281.369 72.3104 -305.144C118.507 -327.949 168.356 -332.792 219.715 -336.844C285.535 -342.038 369.083 -381.424 412.88 -332.018C457.935 -281.194 406.048 -201.31 399.82 -133.678C395.679 -88.7194 394.135 -46.317 382.55 -2.68C368.135 51.6174 373.1 123.327 324.232 151.04C275.433 178.714 218.732 122.276 162.632 122.037C93.5849 121.742 20.3777 187.044 -37.5683 149.496C-93.687 113.131 -85.1022 27.0177 -87.152 -39.8215Z"
                              fill="url(#paint0_linear)" fill-opacity="0.4"></path>
                        <path fill-rule="evenodd" clip-rule="evenodd"
                              d="M-188.911 -99.6179C-180.859 -164.877 -158.829 -227.486 -116.742 -278.006C-77.6303 -324.955 -21.7855 -351.835 36.4978 -370.192C92.4006 -387.799 149.286 -384.577 207.733 -380.204C282.636 -374.6 383.414 -404.355 424.066 -341.195C465.884 -276.222 393.661 -195.431 374.9 -120.476C362.428 -70.6498 353.32 -23.2462 332.709 23.8C307.062 82.3393 300.177 163.824 240.418 186.486C180.743 209.115 126.807 135.805 63.777 125.782C-13.8004 113.447 -107.459 174.137 -166.079 121.848C-222.85 71.2077 -198.227 -24.1155 -188.911 -99.6179Z"
                              fill="url(#paint1_linear)" fill-opacity="0.3"></path>
                        <defs>
                            <linearGradient id="paint0_linear" x1="403.713" y1="80.0373" x2="-60.6291" y2="-29.7743"
                                            gradientUnits="userSpaceOnUse">
                                <stop stop-color="#9EE6F7" stop-opacity="0"></stop>
                                <stop offset="1" stop-color="#9EE6F7" stop-opacity="0.46"></stop>
                            </linearGradient>
                            <linearGradient id="paint1_linear" x1="342.121" y1="120.477" x2="269" y2="-1.00001"
                                            gradientUnits="userSpaceOnUse">
                                <stop stop-color="#9EE6F7" stop-opacity="0"></stop>
                                <stop offset="1" stop-color="#9EE6F7" stop-opacity="0.46"></stop>
                            </linearGradient>
                        </defs>
                    </svg>
                    <svg data-v-2d9bd78e="" width="576" height="657" viewBox="0 0 576 657" fill="none"
                         xmlns="http://www.w3.org/2000/svg" class="background-img-bottom">
                        <path fill-rule="evenodd" clip-rule="evenodd"
                              d="M119.005 490.408C104.348 426.309 103.735 359.939 126.098 298.105C146.88 240.642 190.23 196.348 238.776 159.237C285.339 123.642 339.92 107.296 396.362 91.4996C468.695 71.2562 553.312 8.95396 613.046 54.4918C674.494 101.336 634.107 201.896 641.998 278.759C647.244 329.854 654.826 377.525 651.472 428.779C647.298 492.553 668.578 571.511 620.111 613.172C571.712 654.774 496.031 604.218 433.356 616.263C356.216 631.089 288.829 720.051 215.905 690.855C145.28 662.579 135.963 564.569 119.005 490.408Z"
                              fill="url(#paint0_linear)" fill-opacity="0.3"></path>
                        <path fill-rule="evenodd" clip-rule="evenodd"
                              d="M207.243 573.011C186.674 518.997 178.054 461.296 189.988 404.743C201.078 352.187 233.418 308.347 271.157 270.126C307.354 233.466 352.877 212.586 400.086 191.958C460.587 165.523 526.658 100.977 584.206 133.341C643.406 166.634 620.5 259.094 636.735 325.044C647.526 368.884 659.935 409.46 663.26 454.486C667.397 510.511 695.542 576.654 658.427 618.825C621.363 660.938 549.321 626.149 496.228 644.271C430.882 666.576 383.059 752.23 316.019 735.699C251.094 719.689 231.041 635.504 207.243 573.011Z"
                              fill="url(#paint1_linear)" fill-opacity="0.4"></path>
                        <path fill-rule="evenodd" clip-rule="evenodd"
                              d="M403.49 282.211C453.064 252.494 508.362 233.896 566.131 235.735C619.816 237.444 668.646 261.602 712.889 292.059C755.324 321.272 783.858 362.431 812.44 405.295C849.068 460.228 924.193 513.966 902.414 576.295C880.011 640.412 784.967 634.064 722.882 661.603C681.612 679.91 643.839 699.238 600.092 710.401C545.658 724.291 485.472 763.592 437.449 734.441C389.492 705.33 411.119 628.307 383.973 579.211C350.563 518.785 257.854 486.712 262.381 417.812C266.766 351.086 346.134 316.591 403.49 282.211Z"
                              fill="url(#paint2_linear)" fill-opacity="0.6"></path>
                        <defs>
                            <linearGradient id="paint0_linear" x1="693.25" y1="516.469" x2="150.817" y2="495.802"
                                            gradientUnits="userSpaceOnUse">
                                <stop stop-color="#9EE6F7" stop-opacity="0"></stop>
                                <stop offset="1" stop-color="#9EE6F7" stop-opacity="0.46"></stop>
                            </linearGradient>
                            <linearGradient id="paint1_linear" x1="710.313" y1="525.732" x2="235.594" y2="573.831"
                                            gradientUnits="userSpaceOnUse">
                                <stop stop-color="#9EE6F7" stop-opacity="0"></stop>
                                <stop offset="1" stop-color="#9EE6F7" stop-opacity="0.46"></stop>
                            </linearGradient>
                            <linearGradient id="paint2_linear" x1="538.194" y1="769.211" x2="407.651" y2="310.266"
                                            gradientUnits="userSpaceOnUse">
                                <stop stop-color="#9EE6F7" stop-opacity="0"></stop>
                                <stop offset="1" stop-color="#9EE6F7" stop-opacity="0.46"></stop>
                            </linearGradient>
                        </defs>
                    </svg>
                </div>
                <div className="container">
                    <div id="rawurl">
                        <div>
                            <div className="row"><img src={logo} alt="logo" /></div>
                            <div className="row" style={{marginLeft: '10px'}}>Enter a long URL to make a TinyURL</div>
                        </div>
                        <div className="clear"></div>
                        <div><TextArea value={this.state.rawurl} onChange={this.rawurlChange}
                                       style={{width: '100%'}} autoSize={{minRows: 2}}/>
                            {/* <input type="text" value={this.state.rawurl} onChange={this.rawurlChange}/>*/}
                        </div>
                    </div>
                    <div id="tinyurl">
                        <div>
                            <div className="row"><img src={pen} alt="pen"/></div>
                            <div className="row" style={{marginLeft: '10px'}}>Customize your link</div>
                        </div>
                        <div className="clear"></div>
                        <div>
                            {/* <Select defaultValue={this.state.baseurl} style={{ width: 120 }} onChange={this.baseurlChange}>
                             <Option>t.vipgp88.com</Option>
                             <Option>q.vipgp88.com</Option>
                             </Select>*/}
                            <select value={this.state.baseurl} onChange={this.baseurlChange}>
                                <option>t.vipgp88.com</option>
                                <option>q.vipgp88.com</option>
                            </select>
                            <input type="text" value={this.state.code} style={{marginLeft: '3px',height:'15px'}} onChange={this.codeChange}/>
                        </div>
                    </div>
                    <div id="result">
                        <div>
                            <div className="row"><img src={pen} alt="pen"/></div>
                            <div className="row" style={{marginLeft: '10px'}}>TinyURL</div>
                        </div>
                        <div className="clear"></div>
                        <div>
                            <TextArea value={this.state.tinyurl} onChange={this.tinyurlChange}
                                      style={{width: '60%'}} autoSize={{minRows: 2}}/>
                           {/* <input type="text" value={this.state.tinyurl} onChange={this.tinyurlChange}/>*/}
                        </div>
                    </div>
                    <br/>
                    <div id="finish">
                        <Button type="primary" shape="round"
                                style={{width: '100%', backgroundColor: 'green', color: 'white'}}
                                onClick={this.handleClick}>Make TinyURL!</Button>
                        {/*<input type="button" value="Make TinyURL!" onClick={this.handleClick}/>*/}
                    </div>
                </div>
            </div>
        );
    }
}

export default Tinyurl;
