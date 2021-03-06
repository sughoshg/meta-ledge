HOMEPAGE = "http://www.denx.de/wiki/U-Boot/WebHome"
SECTION = "bootloaders"
DEPENDS += "flex-native bison-native"

LICENSE = "GPLv2+"
LIC_FILES_CHKSUM = "file://Licenses/README;md5=5a7450c57ffe5ae63fd732446b988025"
PE = "1"

# We use the revision in order to avoid having to fetch it from the
# repo during parse

PV = "2020.10-rc1"

SRC_URI = "git://git.denx.de/u-boot.git"
SRCREV = "9f04a634ef331b4fc6b3e677f276156192a413c7"

SRC_URI += " \
	file://0001-efi-change-usage-syntax.patch \
	file://0002-STM32mp157c-DK2.patch \
	file://0003-ti-am572x-enable-boot_distrocmd.patch \
        file://ledge_stm32mp157c_dk2_trusted_defconfig \
    "

S = "${WORKDIR}/git"
B = "${WORKDIR}/build"

SRC_URI_append_ledge-qemuarm = " file://ledge-qemuarm_defconfig"
SRC_URI_append_ledge-qemuarm64 = " file://ledge-qemuarm64_defconfig"

PACKAGE_ARCH = "${MACHINE_ARCH}"

require recipes-bsp/u-boot/u-boot.inc
PROVIDES += "u-boot virtual/bootloader"
RPROVIDES_${PN} += "u-boot virtual/bootloader"

DEPENDS += "bc-native dtc-native"

do_compile_prepend() {
    for conf in ${UBOOT_MACHINE};
    do
        if [ -f ${WORKDIR}/$conf ] ;
        then
            cp ${WORKDIR}/$conf ${S}/configs/
        fi
    done
}

# -----------------------------------------------------------------------------
# Append deploy to handle specific device tree binary deployement
#
SPL_BINARY_LEDGE_ledge-stm32mp157c-dk2 = "spl/u-boot-spl.stm32"
do_deploy_append() {
if [ -n "${SPL_BINARY_LEDGE}" ]; then
    # Clean deploydir from any available binary first
    # This allows to only install the devicetree binary ones
    rm -rf ${DEPLOYDIR}

    # Install destination folder
    install -d ${DEPLOYDIR}

    if [ -n "${UBOOT_CONFIG}" ]; then
        unset i j k
        for config in ${UBOOT_MACHINE}; do
            i=$(expr $i + 1);
            for type in ${UBOOT_CONFIG}; do
                j=$(expr $j + 1);
                if [ $j -eq $i ]; then
                    for binary in ${UBOOT_BINARIES}; do
                        binarysuffix=$(echo ${binary} | cut -d'.' -f2)
                        k=$(expr $k + 1);
                        if [ $k -eq $i ]; then
                            if [ -f ${B}/${config}/${binary} ];
                            then
                                install -m 644 ${B}/${config}/${binary} ${DEPLOYDIR}/u-boot-${type}.${binarysuffix}
                            fi
                            # As soon as SPL binary exists, install it
                            # This allow to mix u-boot configuration, with and without SPL
                            if [ -f ${B}/${config}/${SPL_BINARY_LEDGE} ]; then
                                install -d ${DEPLOYDIR}/spl
                                install -m 644 ${B}/${config}/${SPL_BINARY_LEDGE} ${DEPLOYDIR}/${SPL_BINARY_LEDGE}-${type}
                            fi
                        fi
                    done
                    unset k
                fi
            done
            unset j
        done
        unset i
    else
            bbfatal "Wrong u-boot-ledge configuration: please make sure to use UBOOT_CONFIG through BOOTSCHEME_LABELS config"
    fi
fi
}
do_deploy_append_ledge-qemuarm() {
    cd ${DEPLOYDIR}
    ln -sf u-boot-ledge-qemuarm.bin bl33.bin
    cd -
}

do_deploy_append_ledge-qemuarm64() {
    cd ${DEPLOYDIR}
    ln -sf u-boot-ledge-qemuarm64.bin bl33.bin
    cd -
}

