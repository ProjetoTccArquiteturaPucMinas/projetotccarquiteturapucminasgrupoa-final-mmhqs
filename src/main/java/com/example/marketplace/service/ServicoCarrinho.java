package com.example.marketplace.service;


import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.example.marketplace.model.CategoriaProduto;
import com.example.marketplace.model.ItemCarrinho;
import com.example.marketplace.model.Produto;
import com.example.marketplace.model.ResumoCarrinho;
import com.example.marketplace.model.SelecaoCarrinho;
import com.example.marketplace.repository.ProdutoRepository;

@Service
public class ServicoCarrinho {

    private final ProdutoRepository repositorioProdutos;

    public ServicoCarrinho(ProdutoRepository repositorioProdutos) {
        this.repositorioProdutos = repositorioProdutos;
    }

    private BigDecimal calcularDescontoPorItem(ItemCarrinho item) {
        BigDecimal desconto = BigDecimal.ZERO;
        CategoriaProduto categoria = item.getProduto().getCategoria();
        switch(categoria)
        {
            case CAPINHA:
                desconto = BigDecimal.valueOf(3);
                break;
            case CARREGADOR:
                desconto = BigDecimal.valueOf(5);
                break;
            case FONE:
                desconto = BigDecimal.valueOf(3);
                break;
            case PELICULA:
                desconto = BigDecimal.valueOf(2);
                break;
            case SUPORTE:
                desconto = BigDecimal.valueOf(2);
                break;
            default:
                break;
        }
        return desconto;
    }

    private BigDecimal calcularDescontoPorQuantidade(List<ItemCarrinho> itens) {
        BigDecimal desconto = BigDecimal.ZERO;
        int quantidadeTotal = itens.stream()
                .mapToInt(ItemCarrinho::getQuantidade)
                .sum();
        if (quantidadeTotal == 2) {
            return BigDecimal.valueOf(5);
        } else if (quantidadeTotal == 3) {
            return BigDecimal.valueOf(7); 
        } else if (quantidadeTotal >= 4) {
            return BigDecimal.valueOf(10); 
        }
        return desconto;
    }

    public ResumoCarrinho construirResumo(List<SelecaoCarrinho> selecoes) {

        List<ItemCarrinho> itens = new ArrayList<>();

        // =========================
        // Monta os itens do carrinho
        // =========================
        for (SelecaoCarrinho selecao : selecoes) {
            Produto produto = repositorioProdutos.buscarPorId(selecao.getProdutoId())
                    .orElseThrow(
                            () -> new IllegalArgumentException("Produto não encontrado: " + selecao.getProdutoId()));

            itens.add(new ItemCarrinho(produto, selecao.getQuantidade()));
        }

        // =========================
        // Calcula subtotal
        // =========================
        BigDecimal subtotal = itens.stream()
                .map(ItemCarrinho::calcularSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);


        // =======================================================
        // Calcula desconto considerando a categoria de cada item
        // ========================================================
        BigDecimal percentualDescontoPorItem = itens.stream()
                .map(item -> calcularDescontoPorItem(item))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // =====================================================
        // Calcula desconto considerando a quantidade de itens
        // ======================================================
        BigDecimal percentualDescontoQuantidade = calcularDescontoPorQuantidade(itens);

        // =================================================================
        // Calcula desconto = valorDescontoPorItem + valorDescontoQuantidade
        // =================================================================
        BigDecimal percentualDesconto = percentualDescontoPorItem.add(percentualDescontoQuantidade);

        BigDecimal valorDesconto = percentualDesconto.divide(BigDecimal.valueOf(100)).multiply(subtotal);

        // =======================
        // Calcula total
        // ========================
        BigDecimal total = subtotal.subtract(valorDesconto);

        return new ResumoCarrinho(itens, subtotal, percentualDesconto, valorDesconto, total);
    }
}
