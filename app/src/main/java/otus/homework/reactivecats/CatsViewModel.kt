package otus.homework.reactivecats

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit

class CatsViewModel(
    private val catsService: CatsService,
    private val localCatFactsGenerator: LocalCatFactsGenerator,
    context: Context
) : ViewModel() {

    private val _catsLiveData = MutableLiveData<Result>()
    val catsLiveData: LiveData<Result> = _catsLiveData
    private val disposable = CompositeDisposable()
    private val defaultErrorMessage = context.resources.getString(R.string.default_error_text)

    init {
        Observable.interval(2, TimeUnit.SECONDS)
            .subscribe {
                getFacts()
            }
            .let { disposable.add(it) }
    }

    private fun getFacts() {
        catsService.getCatFact()
            .subscribeOn(Schedulers.io())
            .onErrorResumeNext {
                localCatFactsGenerator.generateCatFact()
            }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                    response -> _catsLiveData.value = Success(response)
                }, {
                    error -> _catsLiveData.value = Error(error.message ?: defaultErrorMessage)
                })
            .let { disposable.add(it)}
    }

    override fun onCleared() {
        disposable.clear()
        super.onCleared()
    }
}

class CatsViewModelFactory(
    private val catsRepository: CatsService,
    private val localCatFactsGenerator: LocalCatFactsGenerator,
    private val context: Context
) :
    ViewModelProvider.NewInstanceFactory() {

    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        CatsViewModel(catsRepository, localCatFactsGenerator, context) as T
}

sealed class Result
data class Success(val fact: Fact) : Result()
data class Error(val message: String) : Result()
object ServerError : Result()