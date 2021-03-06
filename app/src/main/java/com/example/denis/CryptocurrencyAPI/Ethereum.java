package com.example.denis.CryptocurrencyAPI;


import com.example.denis.POJO.ChainSo.Result;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.subgraph.orchid.encoders.Hex;


import org.web3j.crypto.Credentials;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Hash;
import org.web3j.crypto.Keys;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.Sign;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.Web3jFactory;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.Request;
import org.web3j.protocol.core.methods.response.EthGetBalance;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;
import org.web3j.protocol.core.methods.response.EthSendRawTransaction;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.http.HttpService;
import org.web3j.rlp.RlpString;
import org.web3j.rlp.RlpType;
import org.web3j.tx.RawTransactionManager;
import org.web3j.utils.Bytes;
import org.web3j.utils.Convert;
import org.web3j.utils.Numeric;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import rx.Observable;


public class Ethereum implements ICryptocurrency {
    private final String infuraToken = "hgAaKEDG9sIpNHqt8UYM";
    private BigInteger balance;
    private String address;
    private Web3j web3j;
    private Retrofit retrofit;
    private RawTransaction rawTransaction;
    private byte[] testKey = Hex.decode("A25E84B8B28314172100B755CF81EE7A116878F4037B325D1ADEACE1C177F174");
    public RawTransaction getRawTransaction() {
        return rawTransaction;
    }

    public void setRawTransaction(RawTransaction rawTransaction) {
        this.rawTransaction = rawTransaction;
    }

    public Ethereum() {
        this.web3j = Web3jFactory.build(new HttpService("https://ropsten.infura.io/" + this.infuraToken));
        this.initAddress();
    }

    public Observable setAddress(String address) {
        System.out.println("SETTING ADDRESS");
        this.address = Keys.toChecksumAddress(address);
        return Observable.just(this.address);
    }

    @Override
    public double getBalance() {
        return 0;
    }

    public void setBalance(BigInteger balance) {
        this.balance = balance;
    }

    @Override
    public Single<Result> getBalanceAsync() {
        return null;
    }



    @Override
    public void initAddress() {

    }

    public Observable<EthGetBalance> balance() {
        System.out.println("MY ADDRESS "  + this.address);
        Request<?, EthGetBalance> request= this.web3j.ethGetBalance(this.address, DefaultBlockParameterName.LATEST);
        return request.observable();
    }

    public String getHash(RawTransaction rawTransaction) {
        byte [] hash = Hash.sha3(TransactionEncoder.encode(rawTransaction, (byte) 3));
        return Numeric.toHexString(hash);
    }

    public Observable<EthSendTransaction> sendETHTransaction(String txHex) {
        System.out.println("GOT THIS HEX TO SEND " + txHex);
        return this.web3j.ethSendRawTransaction("0x" + txHex).observable();
    }

    public Observable<RawTransaction> createTransaction(String paymentAddress, String amount) {
            return getNonce()
                    .map(nonce -> {
                        System.out.println("NONCE " + nonce.getTransactionCount());
                        RawTransaction transaction = RawTransaction.createEtherTransaction(nonce.getTransactionCount(), Convert.toWei("10", Convert.Unit.GWEI).toBigInteger(),
                                new BigInteger("21000"), paymentAddress, Convert.toWei(amount, Convert.Unit.ETHER).toBigInteger());
                        setRawTransaction(transaction);
                       // ECKeyPair ecKeyPair = new ECKeyPair(new BigInteger("F4A4CC6890E11FE3089E5E7CD4E76EE0AE37D6C348811703443B3623801F8534"), new BigInteger("029D591D0E636D104DCB0D16CA84ECACDEC748F0BDC96B4ADB0A16809CDD77A000"));
                        byte[] signedMessage = TransactionEncoder.signMessage(rawTransaction, (byte) 3,Credentials.create("0xF4A4CC6890E11FE3089E5E7CD4E76EE0AE37D6C348811703443B3623801F8534"));
                        String hexValue = Numeric.toHexString(signedMessage);
                        System.out.println("HEX VALUE " + hexValue);
                        return transaction;

                    });

                //.map(rawTransaction -> Hash.sha3(TransactionEncoder.encode(rawTransaction)).toString())
                //.flatMap(hash -> signTx(hash));
    }

    public Observable createSignedTransaction(String signature, String paymentAddress, String amount) {
        byte [] sign = signature.getBytes();

        return getNonce()
                .map(nonce -> RawTransaction.createEtherTransaction(nonce.getTransactionCount(), Convert.toWei("10", Convert.Unit.GWEI).toBigInteger(),
                        new BigInteger("21000"),paymentAddress, Convert.toWei(amount, Convert.Unit.ETHER).toBigInteger()));
    }

    public Observable<String> signTx(String hash) {
        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        OkHttpClient client = new OkHttpClient.Builder().addInterceptor(interceptor).build();
        Gson gson = new GsonBuilder().setLenient().create();
        Retrofit fit = new Retrofit.Builder().baseUrl("http://10.0.2.2:3000").client(client)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .addCallAdapterFactory(RxJavaCallAdapterFactory.createWithScheduler(rx.schedulers.Schedulers.io()))
                .build();
        return fit.create(ITestConnect.class).getETHSignature(new Bitcoin.BodySign(hash.substring(2)));
    }

    private Observable<EthGetTransactionCount> getNonce() {
        return this.web3j.ethGetTransactionCount(this.address, DefaultBlockParameterName.LATEST).observable();
    }

    public List<RlpType> asRlpValues (RawTransaction rawTransaction,
                                      Sign.SignatureData signatureData) {
        List<RlpType> result = new ArrayList<RlpType>();

        result.add(RlpString.create(rawTransaction.getNonce()));
        result.add(RlpString.create(rawTransaction.getGasPrice()));
        result.add(RlpString.create(rawTransaction.getGasLimit()));

        // an empty to address (contract creation) should not be encoded as a numeric 0 value
        String to = rawTransaction.getTo();
        if (to != null && to.length() > 0) {
            // addresses that start with zeros should be encoded with the zeros included, not
            // as numeric values
            result.add(RlpString.create(Numeric.hexStringToByteArray(to)));
        } else {
            result.add(RlpString.create(""));
        }

        result.add(RlpString.create(rawTransaction.getValue()));

        // value field will already be hex encoded, so we need to convert into binary first
        byte[] data = Numeric.hexStringToByteArray(rawTransaction.getData());
        result.add(RlpString.create(data));

        if (signatureData != null) {
            result.add(RlpString.create(signatureData.getV()));
            result.add(RlpString.create(Bytes.trimLeadingZeroes(signatureData.getR())));
            result.add(RlpString.create(Bytes.trimLeadingZeroes(signatureData.getS())));
        }

        return result;
    }


    public Observable<String> initAddresss() {
        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        OkHttpClient client = new OkHttpClient.Builder().addInterceptor(interceptor).build();
        Gson gson = new GsonBuilder().setLenient().create();
        Retrofit fit = new Retrofit.Builder().baseUrl("http://10.0.2.2:3000").client(client)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .addCallAdapterFactory(RxJavaCallAdapterFactory.createWithScheduler(rx.schedulers.Schedulers.io()))
                .build();
        return fit.create(ITestConnect.class).getETHAddress();
    }

    @Override
    public HashMap<String, String> getLastTransactions() {
        return null;
    }

    @Override
    public void sendTransaction() {

    }

    @Override
    public String getAddress() {
        return this.address;
    }
}
