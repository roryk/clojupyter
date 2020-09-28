KDIR=$PREFIX/share/jupyter/kernels/${PKG_NAME}-${PKG_VERSION}
LIBDIR=$PREFIX/lib/${PKG_NAME}-${PKG_VERSION}

mkdir -p $KDIR && \
mkdir -p $LIBDIR && \
cp "${SRC_DIR}/${PKG_NAME}-${PKG_VERSION}.jar" $LBIDIR && \
cp -r "${SRC_DIR}/lib" $LIBDIR/. && \
cp "${SRC_DIR}/logo-64x64.png" $KDIR && \
bash "${RECIPE_DIR}/../bin/kernel.json --identity ${PKG_NAME}-${PKG_VERSION} ../../../../../Library > ${KDIR}/kernel.json"
