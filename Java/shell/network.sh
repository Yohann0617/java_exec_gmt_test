#!/bin/bash
#设置网卡的IP地址

#变量传参
ETH=$1
IP=$2
MASK=$3
GW=$4
OB=$5
 
#写入到配置文件
echo 'DEVICE='$ETH'
ONBOOT='$OB'
TYPE='Ethernet'
BOOTPROTO='static'
IPADDR='$IP'
GATEWAY='$GW'
NETMASK='$MASK'' >/etc/sysconfig/network-scripts/ifcfg-$ETH
 
#服务重启
ifdown $ETH
ifup $ETH
#service network restart
